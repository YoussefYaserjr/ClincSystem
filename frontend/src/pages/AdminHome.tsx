import { useCallback, useEffect, useState } from 'react';
import { adminApi } from '../api/api';
import { ErrorBox, Loading } from '../components/ui';
import type { DoctorResponse, PageResponse, StatsResponse } from '../types';

export default function AdminHome() {
  const [stats, setStats] = useState<StatsResponse | null>(null);
  const [pending, setPending] = useState<PageResponse<DoctorResponse> | null>(null);
  const [approved, setApproved] = useState<PageResponse<DoctorResponse> | null>(null);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      const [s, p, a] = await Promise.all([
        adminApi.stats(),
        adminApi.doctors({ approved: false, page: 0, size: 50 }),
        adminApi.doctors({ approved: true, page: 0, size: 50 }),
      ]);
      setStats(s);
      setPending(p);
      setApproved(a);
      setError('');
    } catch {
      setError('Could not load dashboard.');
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function act(id: number, action: 'approve' | 'reject') {
    try {
      if (action === 'approve') await adminApi.approve(id);
      else await adminApi.reject(id);
      await load();
    } catch {
      setError('Action failed.');
    }
  }

  async function del(id: number, name: string) {
    if (!window.confirm(`Delete doctor "${name}" permanently?`)) return;
    try {
      await adminApi.deleteDoctor(id);
      await load();
    } catch {
      setError('Could not delete doctor. It may still have appointments, schedules or records.');
    }
  }

  if (!stats || !pending || !approved) return <Loading label="Loading dashboard..." />;

  return (
    <div>
      <h2>Dashboard</h2>
      <ErrorBox message={error} />
      <div className="stats">
        <div className="stat">
          <strong>{stats.totalUsers}</strong> users
        </div>
        <div className="stat">
          <strong>{stats.totalDoctors}</strong> doctors
        </div>
        <div className="stat">
          <strong>{stats.pendingDoctors}</strong> pending approval
        </div>
        <div className="stat">
          <strong>{stats.totalAppointments}</strong> appointments
        </div>
        <div className="stat">
          <strong>{stats.todayAppointments}</strong> today
        </div>
        <div className="stat">
          <strong>{stats.totalMedicalRecords}</strong> records
        </div>
      </div>

      <h2>Pending doctors</h2>
      {pending.content.length === 0 ? (
        <p className="muted">No doctors waiting for approval.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Specialty</th>
              <th>Location</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {pending.content.map((d) => (
              <tr key={d.id}>
                <td>{d.name}</td>
                <td>{d.email}</td>
                <td>{d.specialty}</td>
                <td>{d.location}</td>
                <td className="actions">
                  <button className="primary" onClick={() => act(d.id, 'approve')}>
                    Approve
                  </button>
                  <button className="ghost" onClick={() => act(d.id, 'reject')}>
                    Reject
                  </button>
                  <button className="ghost" onClick={() => del(d.id, d.name)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <h2>Approved doctors</h2>
      {approved.content.length === 0 ? (
        <p className="muted">No approved doctors.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Specialty</th>
              <th>Location</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {approved.content.map((d) => (
              <tr key={d.id}>
                <td>{d.name}</td>
                <td>{d.email}</td>
                <td>{d.specialty}</td>
                <td>{d.location}</td>
                <td className="actions">
                  <button className="ghost" onClick={() => del(d.id, d.name)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
