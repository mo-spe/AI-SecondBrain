import { Router } from 'express';
import { getAiStatus } from '../services/ai.service.js';

export const healthRouter = Router();

healthRouter.get('/', (_request, response) => {
  response.json({ status: 'ok', service: 'feynman-course-server', ai: getAiStatus() });
});
