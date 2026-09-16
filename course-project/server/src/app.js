import express from 'express';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import { env } from './config/env.js';
import { healthRouter } from './routes/health.routes.js';
import { authRouter } from './routes/auth.routes.js';
import { knowledgeRouter } from './routes/knowledge.routes.js';
import { feynmanRouter } from './routes/feynman.routes.js';
import { ragRouter } from './routes/rag.routes.js';
import { graphRouter } from './routes/graph.routes.js';
import { demoRouter } from './routes/demo.routes.js';
import { quizRouter } from './routes/quiz.routes.js';
import { notFound, errorHandler } from './middleware/error.js';

export const createApp = () => {
  const app = express();
  const publicDirectory = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../public');

  app.use(helmet());
  app.use(cors({
    origin: (origin, callback) => {
      const isLocalDesktop = !origin || origin === 'null';
      const isConfiguredWebClient = env.clientOrigin === '*' || origin === env.clientOrigin;
      return callback(null, isLocalDesktop || isConfiguredWebClient);
    }
  }));
  app.use(express.json({ limit: '1mb' }));
  app.use(morgan('dev'));
  app.use('/api/health', healthRouter);
  app.use('/api/auth', authRouter);
  app.use('/api/knowledge', knowledgeRouter);
  app.use('/api/feynman', feynmanRouter);
  app.use('/api/rag', ragRouter);
  app.use('/api/graph', graphRouter);
  app.use('/api/demo', demoRouter);
  app.use('/api/quiz', quizRouter);
  if (env.serveWeb) {
    app.use(express.static(publicDirectory));
    app.get('*', (request, response, next) => {
      if (request.path.startsWith('/api/')) return next();
      return response.sendFile(path.join(publicDirectory, 'index.html'));
    });
  }
  app.use(notFound);
  app.use(errorHandler);

  return app;
};
