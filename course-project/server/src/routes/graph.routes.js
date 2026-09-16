import { Router } from 'express';
import { requireAuth } from '../middleware/auth.js';
import { buildKnowledgeGraph } from '../services/graph.service.js';

export const graphRouter = Router();
graphRouter.use(requireAuth);

graphRouter.get('/knowledge-map', async (request, response, next) => {
  try {
    return response.json(await buildKnowledgeGraph(request.user.id));
  } catch (error) {
    return next(error);
  }
});
