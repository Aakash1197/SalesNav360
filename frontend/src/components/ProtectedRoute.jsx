import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export default function ProtectedRoute({ requireAdmin = false, requiredRoles = [] }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <main className="page-center"><p className="muted">Checking your Apex Finodeal LLP session…</p></main>;
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  if (requireAdmin && !user.roles?.includes('ADMIN')) {
    return <Navigate to="/dashboard" replace />;
  }
  if (requiredRoles.length && !requiredRoles.some((role) => user.roles?.includes(role))) {
    return <Navigate to="/dashboard" replace />;
  }
  return <Outlet />;
}
