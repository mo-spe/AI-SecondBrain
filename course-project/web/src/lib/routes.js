import { cachedRequest } from './dataCache.js';

export const routeImports = {
  login: () => import('../pages/LoginPage.jsx'),
  dashboard: () => import('../pages/DashboardPage.jsx'),
  knowledge: () => import('../pages/KnowledgePage.jsx'),
  knowledgeDetail: () => import('../pages/KnowledgeDetailPage.jsx'),
  feynman: () => import('../pages/FeynmanPage.jsx'),
  quiz: () => import('../pages/QuizPage.jsx'),
  rag: () => import('../pages/RagPage.jsx'),
  graph: () => import('../pages/GraphPage.jsx'),
  universe: () => import('../pages/UniversePage.jsx'),
  earth: () => import('../pages/EarthPage.jsx')
};

const routeKeys = {
  '/': 'dashboard',
  '/knowledge': 'knowledge',
  '/feynman': 'feynman',
  '/quiz': 'quiz',
  '/rag': 'rag',
  '/graph': 'graph',
  '/universe': 'universe',
  '/earth': 'earth'
};

export const preloadRoute = (path) => {
  const pathname = path.split('?')[0];
  const routeKey = routeKeys[pathname];
  if (!routeKey) return Promise.resolve();
  const requests = [routeImports[routeKey]()];
  // 意图明确时让数据请求与页面代码并行，复用缓存去重；预加载失败由正式页面继续处理。
  if (['dashboard', 'knowledge', 'feynman', 'quiz'].includes(routeKey)) requests.push(cachedRequest('knowledge', '/knowledge'));
  else if (['graph', 'universe', 'earth'].includes(routeKey)) requests.push(cachedRequest('knowledge-graph', '/graph/knowledge-map'));
  return Promise.allSettled(requests);
};
