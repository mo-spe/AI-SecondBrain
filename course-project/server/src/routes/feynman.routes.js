import { Router } from 'express';
import multer from 'multer';
import { FeynmanAttempt } from '../models/FeynmanAttempt.js';
import { Knowledge } from '../models/Knowledge.js';
import { requireAuth } from '../middleware/auth.js';
import { evaluateFeynmanWithAi, isAiConfigured } from '../services/ai.service.js';
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

feynmanRouter.post('/evaluate', async (request, response, next) => {
  try {
    const knowledge = await findOwnedKnowledge(request, request.body.knowledgeId);
    const transcript = String(request.body.transcript || '').trim();
    if (!knowledge) return response.status(404).json({ message: '知识点不存在' });
    if (!transcript) return response.status(400).json({ message: '转录内容不能为空' });

    if (isAiConfigured()) return response.json(await evaluateFeynmanWithAi(knowledge, transcript));

    const hasDefinition = transcript.length >= 30;
    const hasExample = /例如|比如|场景|像/.test(transcript);
    const score = Math.min(96, 48 + (hasDefinition ? 28 : 12) + (hasExample ? 14 : 0));
    const strengths = hasDefinition ? ['解释已经包含核心含义'] : ['已经开始用自己的话组织解释'];
    const weaknesses = hasExample ? ['可以再补充一个反例帮助区分边界'] : ['补充一个具体例子，能让理解更容易被验证'];
    const evaluation = hasExample
      ? '你的解释同时覆盖了概念和使用场景，下一次可以继续压缩表达，让核心判断更快被听懂。'
      : '你已经说出了自己的理解，下一次尝试加入一个具体例子或反例，检查概念边界是否清晰。';

    return response.json({ score, evaluation, polishedText: transcript, strengths, weaknesses, provider: 'mock' });
  } catch (error) {
    return next(error);
  }
});

feynmanRouter.post('/attempts', async (request, response, next) => {
  try {
    const knowledge = await findOwnedKnowledge(request, request.body.knowledgeId);
    if (!knowledge) return response.status(404).json({ message: '知识点不存在' });

    const clientAttemptId = String(request.body.clientAttemptId || '').trim();
    if (clientAttemptId) {
      const existingAttempt = await FeynmanAttempt.findOne({ user: request.user.id, clientAttemptId });
      if (existingAttempt) return response.status(200).json({ attempt: existingAttempt, duplicate: true });
    }

    try {
      const attempt = await FeynmanAttempt.create({
        user: request.user.id,
        knowledge: knowledge.id,
        clientAttemptId: clientAttemptId || undefined,
        transcript: request.body.transcript,
        polishedText: request.body.polishedText,
        score: request.body.score,
        evaluation: request.body.evaluation,
        strengths: request.body.strengths,
        weaknesses: request.body.weaknesses
      });
      knowledge.reviewScore = request.body.score;
      knowledge.lastReviewedAt = new Date();
      await knowledge.save();
      return response.status(201).json({ attempt });
    } catch (error) {
      if (error?.code === 11000 && clientAttemptId) {
        const existingAttempt = await FeynmanAttempt.findOne({ user: request.user.id, clientAttemptId });
        if (existingAttempt) return response.status(200).json({ attempt: existingAttempt, duplicate: true });
      }
      throw error;
    }
  } catch (error) {
    return next(error);
  }
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
