import { Navigate, Route, Routes } from 'react-router-dom';
import DashboardPage from './components/DashboardPage';
import ClientsPage from './components/ClientsPage';
import LeadsPage from './components/LeadsPage';
import LoginPage from './components/LoginPage';
import ProtectedRoute from './components/ProtectedRoute';
import AppShell from './components/AppShell';
import AdminUsersPage from './components/AdminUsersPage';
import TasksPage from './components/TasksPage';
import PoliciesPage from './components/PoliciesPage';
import QuotationsPage from './components/QuotationsPage';
import AnalyticsPage from './components/AnalyticsPage';
import CalendarPage from './components/CalendarPage';
import EmailPage from './components/EmailPage';
import ReportsPage from './components/ReportsPage';
import LandingPage from './components/LandingPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/clients" element={<ClientsPage />} />
          <Route path="/leads" element={<LeadsPage />} />
          <Route path="/tasks" element={<TasksPage />} />
          <Route path="/analytics" element={<AnalyticsPage />} />
          <Route path="/calendar" element={<CalendarPage />} />
          <Route path="/email" element={<EmailPage />} />
          <Route path="/reports" element={<ReportsPage />} />
          <Route element={<ProtectedRoute requiredRoles={['ADMIN', 'SALES_REPRESENTATIVE', 'SALES_MANAGER', 'OPERATIONS']} />}>
            <Route path="/policies" element={<PoliciesPage />} />
            <Route path="/quotations" element={<QuotationsPage />} />
          </Route>
          <Route element={<ProtectedRoute requireAdmin />}>
            <Route path="/admin/users" element={<AdminUsersPage />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
