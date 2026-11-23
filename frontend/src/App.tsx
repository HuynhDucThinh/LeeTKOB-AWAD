import { Routes, Route, Navigate } from 'react-router-dom';
import { useGetMeQuery } from './services/authApi';
import { useAppSelector } from './store/hooks';
import { AuthPage } from './pages/AuthPage';
import { WelcomePage } from './pages/WelcomePage';

export default function App() {
  const { isLoading } = useGetMeQuery();
  const { isAuthenticated } = useAppSelector((state) => state.auth);

  if (isLoading) {
    return (
      <div className="loading-screen">
        <div className="spinner"></div>
        <p>Đang tải thông tin LeeTKOB...</p>
      </div>
    );
  }

  return (
    <Routes>
      <Route
        path="/"
        element={
          isAuthenticated ? <Navigate to="/welcome" replace /> : <AuthPage />
        }
      />
      <Route
        path="/welcome"
        element={
          isAuthenticated ? <WelcomePage /> : <Navigate to="/" replace />
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
