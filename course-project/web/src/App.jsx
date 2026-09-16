import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext.jsx';
import AppShell from './components/AppShell.jsx';
import LoginPage from './pages/LoginPage.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import KnowledgePage from './pages/KnowledgePage.jsx';
import FeynmanPage from './pages/FeynmanPage.jsx';
import RagPage from './pages/RagPage.jsx';
import GraphPage from './pages/GraphPage.jsx';
import UniversePage from './pages/UniversePage.jsx';
import EarthPage from './pages/EarthPage.jsx';
import QuizPage from './pages/QuizPage.jsx';

function ProtectedRoute({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="screen-center">正在恢复学习空间…</div>;
  return user ? children : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <Routes>
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
    </Routes>
  );
}
