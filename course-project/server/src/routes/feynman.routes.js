import { Router } from 'express';
import multer from 'multer';
import { FeynmanAttempt } from '../models/FeynmanAttempt.js';
import { Knowledge } from '../models/Knowledge.js';
import { requireAuth } from '../middleware/auth.js';
import { evaluateFeynmanWithAi, isAiConfigured } from '../services/ai.service.js';

export const feynmanRouter = Router();
feynmanRouter.use(requireAuth);

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 10 * 1024 * 1024 },
  fileFilter: (_request, file, callback) => {
    const accepted = ['audio/webm', 'audio/wav', 'audio/mpeg', 'audio/ogg'].includes(file.mimetype);
    callback(accepted ? null : new Error('暂不支持这个音频格式，请使用 WebM、WAV、MP3 或 OGG'), accepted);
  }
});

const findOwnedKnowledge = (request, knowledgeId) => Knowledge.findOne({ _id: knowledgeId, owner: request.user.id });

feynmanRouter.post('/transcribe', upload.single('audio'), async (request, response, next) => {
  try {
    if (!request.file) return response.status(400).json({ message: '请先录制一段音频' });
    const knowledge = await findOwnedKnowledge(request, request.body.knowledgeId);
    if (!knowledge) return response.status(404).json({ message: '知识点不存在' });
    const transcript = String(request.body.transcript || '').trim()
      || `我来解释一下“${knowledge.title}”：${knowledge.content}`;
    return response.json({ transcript, provider: 'mock', durationMs: 0 });
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

    const attempt = await FeynmanAttempt.create({
      user: request.user.id,
      knowledge: knowledge.id,
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
