import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/api';
import { saveAuthResponse, useAuth } from '../auth';
import { ErrorBox } from '../components/ui';

export default function Register() {
  const { saveSession } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    phone: '',
    role: 'PATIENT' as 'PATIENT' | 'DOCTOR',
    specialty: '',
    location: '',
  });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  function set<K extends keyof typeof form>(key: K, value: (typeof form)[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const payload = { ...form };
      if (form.role === 'PATIENT') {
        payload.specialty = '';
        payload.location = '';
      }
      const res = await authApi.register(payload);
      saveSession(saveAuthResponse(res));
      navigate('/');
    } catch (err) {
      setError('Registration failed. The email may already be in use.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-card">
      <h1>Create account</h1>
      <p className="muted">Register as a patient or a doctor</p>
      <form onSubmit={submit}>
        <label>
          Name
          <input value={form.name} onChange={(e) => set('name', e.target.value)} required />
        </label>
        <label>
          Email
          <input
            type="email"
            value={form.email}
            onChange={(e) => set('email', e.target.value)}
            required
          />
        </label>
        <label>
          Password
          <input
            type="password"
            value={form.password}
            onChange={(e) => set('password', e.target.value)}
            required
            minLength={8}
          />
        </label>
        <label>
          Phone
          <input value={form.phone} onChange={(e) => set('phone', e.target.value)} required />
        </label>
        <label>
          I am a
          <select value={form.role} onChange={(e) => set('role', e.target.value as 'PATIENT' | 'DOCTOR')}>
            <option value="PATIENT">Patient</option>
            <option value="DOCTOR">Doctor</option>
          </select>
        </label>
        {form.role === 'DOCTOR' && (
          <>
            <label>
              Specialty
              <input value={form.specialty} onChange={(e) => set('specialty', e.target.value)} required />
            </label>
            <label>
              Location
              <input value={form.location} onChange={(e) => set('location', e.target.value)} required />
            </label>
          </>
        )}
        <ErrorBox message={error} />
        <button className="primary" type="submit" disabled={busy}>
          {busy ? 'Creating...' : 'Create account'}
        </button>
      </form>
      <p className="muted">
        Already have an account? <Link to="/login">Sign in</Link>
      </p>
    </div>
  );
}
