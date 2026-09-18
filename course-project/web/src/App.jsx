import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext.jsx';
import AppShell from './components/AppShell.jsx';
import PageLoading from './components/PageLoading.jsx';
import { routeImports } from './lib/routes.js';

const LoginPage = lazy(routeImports.login);
const DashboardPage = lazy(routeImports.dashboard);
const KnowledgePage = lazy(routeImports.knowledge);
const FeynmanPage = lazy(routeImports.feynman);
const RagPage = lazy(routeImports.rag);
const GraphPage = lazy(routeImports.graph);
const UniversePage = lazy(routeImports.universe);
const EarthPage = lazy(routeImports.earth);
const QuizPage = lazy(routeImports.quiz);

function ProtectedRoute({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="screen-center">正在恢复学习空间…</div>;
  return user ? children : <Navigate to="/login" replace />;
}

export default function App() {
  return <Suspense fallback={<PageLoading label="正在打开学习模块…" />}><Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route
      path="/*"
      element={
        <ProtectedRoute>
          <AppShell />
        </ProtectedRoute>
      }
    >
      <Route index element={<DashboardPage />} />
      <Route path="knowledge" element={<KnowledgePage />} />
      <Route path="feynman" element={<FeynmanPage />} />
      <Route path="quiz" element={<QuizPage />} />
      <Route path="rag" element={<RagPage />} />
      <Route path="graph" element={<GraphPage />} />
      <Route path="universe" element={<UniversePage />} />
      <Route path="earth" element={<EarthPage />} />
    </Route>
  </Routes></Suspense>;
}
