import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import type { AuthResponse, Role } from './types';

export interface Session {
  userId: number;
  role: Role;
  token: string;
  refreshToken: string;
}

const STORAGE_KEY = 'clinic.user';

function loadSession(): Session | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    const s = JSON.parse(raw) as Session;
    return s && s.token ? s : null;
  } catch {
    return null;
  }
}

let currentSession: Session | null = loadSession();
const listeners = new Set<(s: Session | null) => void>();

export function getSession(): Session | null {
  return currentSession;
}

export function updateSession(s: Session | null) {
  currentSession = s;
  if (s) localStorage.setItem(STORAGE_KEY, JSON.stringify(s));
  else localStorage.removeItem(STORAGE_KEY);
  listeners.forEach((l) => l(s));
}

export function subscribeSession(listener: (s: Session | null) => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

const AuthContext = createContext<{
  session: Session | null;
  saveSession: (s: Session | null) => void;
}>({ session: null, saveSession: () => {} });

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(currentSession);

  useEffect(() => subscribeSession(setSession), []);

  const saveSession = (s: Session | null) => {
    updateSession(s);
  };

  return (
    <AuthContext.Provider value={{ session, saveSession }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}

export function saveAuthResponse(auth: AuthResponse): Session {
  return {
    userId: auth.userId,
    role: auth.role,
    token: auth.token,
    refreshToken: auth.refreshToken,
  };
}

export function Protected({ role, children }: { role?: Role; children: ReactNode }) {
  const { session } = useAuth();
  if (!session) return <Navigate to="/login" replace />;
  if (role && session.role !== role) return <Navigate to="/" replace />;
  return <>{children}</>;
}
