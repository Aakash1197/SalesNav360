import { useState } from 'react';
import { ArrowLeft, ArrowRight, ShieldCheck } from 'lucide-react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
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
    return 'Cannot reach the Apex Finodeal LLP service. Check that the backend is running and the API URL is correct.';
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

  if (loading) return <main className="page-center"><p className="muted">Checking your Apex Finodeal LLP session…</p></main>;
  if (user) return <Navigate to="/dashboard" replace />;

  const submit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await login(credentials);
      navigate(location.state?.from?.pathname || '/dashboard', { replace: true });
    } catch (requestError) {
      setError(getLoginErrorMessage(requestError));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="page-center login-page">
      <section className="login-story" aria-label="Apex Finodeal LLP">
        <img className="login-story-image" src="/loan-advisor-background.png" alt="" />
        <Link className="login-home-link" to="/"><ArrowLeft size={16} /> Back to home</Link>
        <Link className="login-story-brand" to="/" aria-label="Apex Finodeal LLP home">
          <img src="/apex-finodeal-mark.svg" alt="" />
          <span><strong>Apex Finodeal LLP</strong><small>FINANCIAL GUIDANCE</small></span>
        </Link>
        <div className="login-story-copy">
          <span className="login-kicker">Support for your next step</span>
          <h2>Clear guidance.<br />More confidence.</h2>
          <p>Access your Apex Finodeal workspace to continue the conversation and keep your financial guidance in one place.</p>
        </div>
        <div className="login-story-footer">
          <ShieldCheck size={17} />
          <span>THOUGHTFUL GUIDANCE <i>·</i> YOUR CHOICES, YOURS</span>
        </div>
      </section>
      <section className="panel login-panel">
        <div className="login-brand">
          <span className="login-brand-icon"><ShieldCheck size={21} /></span>
          <span className="login-panel-label">SECURE CLIENT WORKSPACE</span>
        </div>
        <div className="login-intro">
          <span className="login-kicker">Welcome back</span>
          <h2>Sign in to continue</h2>
          <p className="muted">Use your account details to open your workspace.</p>
        </div>
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
            {submitting ? 'Signing in…' : <>Sign in <ArrowRight size={17} /></>}
          </button>
        </form>
        <p className="login-security-note"><ShieldCheck size={15} /> Your account details are protected.</p>
        <Link className="login-mobile-home-link" to="/"><ArrowLeft size={15} /> Return to homepage</Link>
      </section>
    </main>
  );
}
