import { Router } from 'express';
import multer from 'multer';
import { FeynmanAttempt } from '../models/FeynmanAttempt.js';
import { Knowledge } from '../models/Knowledge.js';
import { requireAuth } from '../middleware/auth.js';
import { polishFeynmanWithAi, isAiConfigured, getAiStatus } from '../services/ai.service.js';
import { evaluateAndSaveFeynman } from '../services/feynman.service.js';
import { isBaiduAsrConfigured, transcribeWithBaidu } from '../services/speech.service.js';

export const feynmanRouter = Router();
feynmanRouter.use(requireAuth);

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 10 * 1024 * 1024 },
  fileFilter: (_request, file, callback) => {
    const accepted = ['audio/wav', 'audio/x-wav', 'audio/wave'].includes(file.mimetype);
    const error = accepted ? null : Object.assign(
      new Error('暂不支持这个音频格式，请使用单声道 16kHz WAV 音频。'),
      { statusCode: 400 }
    );
    callback(error, accepted);
  }
});

const findOwnedKnowledge = (request, knowledgeId) => Knowledge.findOne({ _id: knowledgeId, owner: request.user.id });

feynmanRouter.post('/transcribe', upload.single('audio'), async (request, response, next) => {
  try {
    if (!request.file) return response.status(400).json({ message: '请先录制一段音频' });
    const knowledge = await findOwnedKnowledge(request, request.body.knowledgeId);
    if (!knowledge) return response.status(404).json({ message: '知识点不存在' });
    if (!isBaiduAsrConfigured()) {
      return response.status(503).json({
        message: '百度语音识别尚未配置，请先填写服务端 .env 中的 BAIDU_APP_ID、BAIDU_API_KEY 和 BAIDU_SECRET_KEY。'
      });
    }

    const result = await transcribeWithBaidu(request.file.buffer, {
      format: 'wav',
      rate: 16000,
      cuid: `feynman-${request.user.id}`
    });
    return response.json({ ...result, durationMs: null });
  } catch (error) {
    return next(error);
  }
});

const readText = (value) => typeof value === 'string' ? value.trim() : '';

feynmanRouter.post('/polish', async (request, response, next) => {
  try {
    const knowledge = await findOwnedKnowledge(request, request.body.knowledgeId);
    if (!knowledge) return response.status(404).json({ message: '知识点不存在' });
    const transcript = readText(request.body.transcript);
    if (!transcript || transcript.length > 30000) return response.status(400).json({ message: '复述原文不能为空，且不能超过30000字符' });
    if (!isAiConfigured()) return response.status(503).json({ message: '请先配置真实 AI Provider，再使用润色功能' });
    return response.json(await polishFeynmanWithAi(transcript));
  } catch (error) { return next(error); }
});

feynmanRouter.post('/evaluate', async (request, response, next) => {
  try {
    const knowledge = await findOwnedKnowledge(request, request.body.knowledgeId);
    if (!knowledge) return response.status(404).json({ message: '知识点不存在' });
    const transcript = readText(request.body.transcript);
    const polishedText = readText(request.body.polishedText);
    const clientAttemptId = readText(request.body.clientAttemptId);
    if (!transcript || !polishedText || transcript.length > 30000 || polishedText.length > 30000 || !clientAttemptId || clientAttemptId.length > 100) {
      return response.status(400).json({ message: '请提供复述原文、润色结果和有效的评价编号' });
    }
    if (!isAiConfigured()) return response.status(503).json({ message: '请先配置真实 AI Provider，再使用评价功能' });
    const attempt = await evaluateAndSaveFeynman({ userId: request.user.id, knowledge, transcript, polishedText, clientAttemptId });
    const item = await findOwnedKnowledge(request, knowledge.id);
    return response.json({
      score: attempt.score, evaluation: attempt.evaluation, polishedText: attempt.polishedText,
      strengths: attempt.strengths, weaknesses: attempt.weaknesses,
      provider: getAiStatus().provider, attempt, item, saved: true
    });
  } catch (error) { return next(error); }
});

// 兼容旧页面的保存按钮，但只确认服务端已评价的记录，不再接受客户端传入的评分。
feynmanRouter.post('/attempts', async (request, response, next) => {
  try {
    const knowledge = await findOwnedKnowledge(request, request.body.knowledgeId);
    if (!knowledge) return response.status(404).json({ message: '知识点不存在' });
    const clientAttemptId = readText(request.body.clientAttemptId);
    if (!clientAttemptId) return response.status(400).json({ message: '请先完成 AI 评价，记录会自动保存' });
    const attempt = await FeynmanAttempt.findOne({ user: request.user.id, knowledge: knowledge.id, clientAttemptId });
    return attempt ? response.json({ attempt, duplicate: true }) : response.status(400).json({ message: '请先完成 AI 评价，记录会自动保存' });
  } catch (error) { return next(error); }
});

feynmanRouter.get('/attempts/:knowledgeId', async (request, response, next) => {
  try {
    const knowledge = await findOwnedKnowledge(request, request.params.knowledgeId);
    if (!knowledge) return response.status(404).json({ message: '知识点不存在' });
    const attempts = await FeynmanAttempt.find({ user: request.user.id, knowledge: knowledge.id }).sort({ createdAt: -1 });
    return response.json({ attempts });
  } catch (error) {
    return next(error);
  }
});
