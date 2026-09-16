import { Router } from 'express';
import { requireAuth } from '../middleware/auth.js';
import { answerFromKnowledge } from '../services/rag.service.js';

export const ragRouter = Router();
ragRouter.use(requireAuth);

ragRouter.post('/ask', async (request, response, next) => {
  try {
    const query = String(request.body.query || '').trim();
    if (query.length < 2) return response.status(400).json({ message: '问题至少需要 2 个字符' });
    const result = await answerFromKnowledge(request.user.id, query);
    return response.json({ query, ...result });
  } catch (error) {
    return next(error);
  }
});
