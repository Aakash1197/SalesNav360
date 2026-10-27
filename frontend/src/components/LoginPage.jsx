import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

function getLoginErrorMessage(error) {
  const status = error.response?.status;
  if (status === 401) {
    return 'Username or password is incorrect, or this account is disabled.';
  }
  if (status === 403) {
    return 'The security token was rejected. Refresh the page and try again.';
  }
  if (!error.response) {
    return 'Cannot reach the SalesNav360 API. Check that the backend is running and the API URL is correct.';
  }
  return error.response.data?.message || `Login failed (HTTP ${status}).`;
}

export default function LoginPage() {
  const { user, loading, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [credentials, setCredentials] = useState({ username: '', password: '' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  if (loading) return <main className="page-center"><p className="muted">Checking your SalesNav360 session…</p></main>;
  if (user) return <Navigate to="/" replace />;

  const submit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await login(credentials);
      navigate(location.state?.from?.pathname || '/', { replace: true });
    } catch (requestError) {
      setError(getLoginErrorMessage(requestError));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="page-center">
      <section className="panel">
        <h1 className="brand">SalesNav360</h1>
        <p className="muted">Sign in using your SalesNav360 account.</p>
        {error && <p className="error-message" role="alert">{error}</p>}
        <form onSubmit={submit}>
          <label className="field">
            <span>Username</span>
            <input
              autoComplete="username"
              required
              value={credentials.username}
              onChange={(event) => setCredentials({ ...credentials, username: event.target.value })}
            />
          </label>
          <label className="field">
            <span>Password</span>
            <input
              type="password"
              autoComplete="current-password"
              required
              value={credentials.password}
              onChange={(event) => setCredentials({ ...credentials, password: event.target.value })}
            />
          </label>
          <button className="primary-button" type="submit" disabled={submitting}>
            {submitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
      </section>
    </main>
  );
}
