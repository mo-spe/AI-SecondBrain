export const routeImports = {
  login: () => import('../pages/LoginPage.jsx'),
  dashboard: () => import('../pages/DashboardPage.jsx'),
  knowledge: () => import('../pages/KnowledgePage.jsx'),
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
  const routeKey = routeKeys[path];
  return routeKey ? routeImports[routeKey]() : Promise.resolve();
};
