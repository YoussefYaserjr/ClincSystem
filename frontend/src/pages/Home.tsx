import { useEffect, useState } from 'react';
import { useAuth } from '../auth';
import { Navigate } from 'react-router-dom';

export default function Home() {
  const { session } = useAuth();
  if (!session) return <Navigate to="/login" replace />;
  if (session.role === 'DOCTOR') return <Navigate to="/doctor" replace />;
  if (session.role === 'ADMIN') return <Navigate to="/admin" replace />;
  return <Navigate to="/patient" replace />;
}
