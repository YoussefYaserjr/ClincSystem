import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { appointmentsApi, doctorsApi, medicalRecordsApi } from '../api/api';
import { useAuth } from '../auth';
import { ErrorBox, Loading, Pagination, StatusBadge, formatDateTime, money } from '../components/ui';
import type { AppointmentResponse, DoctorResponse, MedicalRecordResponse, PageResponse } from '../types';

const PAGE_SIZE = 10;

export default function PatientHome() {
  const { session } = useAuth();
  const [specialty, setSpecialty] = useState('');
  const [location, setLocation] = useState('');
  const [doctors, setDoctors] = useState<PageResponse<DoctorResponse> | null>(null);
  const [page, setPage] = useState(0);
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [records, setRecords] = useState<MedicalRecordResponse[]>([]);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');

  const loadDoctors = useCallback(async () => {
    try {
      const data = await doctorsApi.list({
        specialty: specialty || undefined,
        location: location || undefined,
        page,
        size: PAGE_SIZE,
      });
      setDoctors(data);
      setError('');
    } catch {
      setError('Could not load doctors.');
    }
  }, [specialty, location, page]);

  const loadAppointments = useCallback(async () => {
    try {
      const data = await appointmentsApi.mine({ page: 0, size: 50 });
      setAppointments(data.content);
    } catch {
      // ignore; appointments list is secondary
    }
  }, []);

  const loadRecords = useCallback(async () => {
    if (!session?.userId) return;
    try {
      const data = await medicalRecordsApi.forPatient(session.userId, { page: 0, size: 50 });
      setRecords(data.content);
    } catch {
      // ignore; records list is secondary
    }
  }, [session?.userId]);

  useEffect(() => {
    loadDoctors();
  }, [loadDoctors]);

  useEffect(() => {
    loadAppointments();
  }, [loadAppointments]);

  useEffect(() => {
    loadRecords();
  }, [loadRecords]);

  async function search(e: FormEvent) {
    e.preventDefault();
    setPage(0);
    await loadDoctors();
  }

  async function cancel(id: number) {
    try {
      await appointmentsApi.cancel(id);
      setMsg('Appointment cancelled.');
      await loadAppointments();
    } catch {
      setError('Could not cancel appointment.');
    }
  }

  return (
    <div>
      <h2>Find a doctor</h2>
      <form className="filters" onSubmit={search}>
        <input
          placeholder="Specialty (e.g. Cardiology)"
          value={specialty}
          onChange={(e) => setSpecialty(e.target.value)}
        />
        <input
          placeholder="Location (e.g. Cairo)"
          value={location}
          onChange={(e) => setLocation(e.target.value)}
        />
        <button className="primary" type="submit">
          Search
        </button>
      </form>
      <ErrorBox message={error} />
      {msg && <div className="success">{msg}</div>}

      {!doctors ? (
        <Loading label="Loading doctors..." />
      ) : (
        <>
          {doctors.content.length === 0 && <p className="muted">No doctors found.</p>}
          <div className="cards">
            {doctors.content.map((d) => (
              <div className="card" key={d.id}>
                <h3>{d.name}</h3>
                <p className="muted">{d.specialty}</p>
                <p>
                  {d.location}
                  {d.experience ? ` · ${d.experience} yrs` : ''}
                </p>
                <p>
                  {money(d.consultationFee)}
                  {d.rating ? ` · rating ${Number(d.rating).toFixed(1)}` : ''}
                </p>
                <Link className="primary" to={`/doctors/${d.id}`}>
                  View slots
                </Link>
              </div>
            ))}
          </div>
          <Pagination page={page} totalPages={doctors.totalPages} onChange={setPage} />
        </>
      )}

      <h2>My appointments</h2>
      {appointments.length === 0 ? (
        <p className="muted">You have no appointments yet.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Doctor</th>
              <th>Time</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {appointments.map((a) => (
              <tr key={a.id}>
                <td>{a.doctorName}</td>
                <td>{formatDateTime(a.appointmentTime)}</td>
                <td>
                  <StatusBadge status={a.status} />
                </td>
                <td>
                  {(a.status === 'PENDING' || a.status === 'CONFIRMED') && (
                    <button className="ghost" onClick={() => cancel(a.id)}>
                      Cancel
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <h2>My medical records</h2>
      {records.length === 0 ? (
        <p className="muted">No medical records yet.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Doctor</th>
              <th>Date</th>
              <th>Diagnosis</th>
              <th>Prescription</th>
              <th>Notes</th>
            </tr>
          </thead>
          <tbody>
            {records.map((r) => (
              <tr key={r.id}>
                <td>{r.doctorName}</td>
                <td>{formatDateTime(r.createdAt)}</td>
                <td>{r.diagnosis}</td>
                <td>{r.prescription || '-'}</td>
                <td>{r.notes || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
