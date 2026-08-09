import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/api';
import { saveAuthResponse, useAuth } from '../auth';
import { ErrorBox } from '../components/ui';

export default function Login() {
  const { saveSession } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const res = await authApi.login(email, password);
      saveSession(saveAuthResponse(res));
      navigate('/');
    } catch {
      setError('Login failed. Check your email and password.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-card">
      <h1>ClinicSystem</h1>
      <p className="muted">Sign in to continue</p>
      <form onSubmit={submit}>
        <label>
          Email
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoComplete="username"
          />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            autoComplete="current-password"
          />
        </label>
        <ErrorBox message={error} />
        <button className="primary" type="submit" disabled={busy}>
          {busy ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
      <p className="muted">
        No account? <Link to="/register">Register</Link>
      </p>
    </div>
  );
}
