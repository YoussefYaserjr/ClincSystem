import { Link, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';

export default function Layout() {
  const { session, saveSession } = useAuth();
  const navigate = useNavigate();

  const home =
    session?.role === 'DOCTOR' ? '/doctor' : session?.role === 'ADMIN' ? '/admin' : '/patient';

  function logout() {
    saveSession(null);
    navigate('/login');
  }

  return (
    <div className="app">
      <nav className="navbar">
        <Link className="brand" to={home}>
          ClinicSystem
        </Link>
        <div className="nav-links">
          {session?.role === 'PATIENT' && (
            <Link to="/patient">Find a doctor</Link>
          )}
          {session?.role === 'DOCTOR' && (
            <Link to="/doctor">My practice</Link>
          )}
          {session?.role === 'ADMIN' && (
            <Link to="/admin">Dashboard</Link>
          )}
          {session && (
            <span className="muted">
              {session.role.toLowerCase()} account
            </span>
          )}
        </div>
        {session && (
          <button className="ghost" onClick={logout}>
            Sign out
          </button>
        )}
      </nav>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
