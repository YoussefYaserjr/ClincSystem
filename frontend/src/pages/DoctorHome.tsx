import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { appointmentsApi, medicalRecordsApi, schedulesApi } from '../api/api';
import { useAuth } from '../auth';
import { ErrorBox, Loading, Pagination, StatusBadge, formatDateTime } from '../components/ui';
import type { AppointmentResponse, MedicalRecordResponse, PageResponse, ScheduleResponse } from '../types';

type Tab = 'slots' | 'appointments' | 'records';

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export default function DoctorHome() {
  const { session } = useAuth();
  const [tab, setTab] = useState<Tab>('slots');

  return (
    <div>
      <h2>My practice</h2>
      <div className="tabs">
        <button className={tab === 'slots' ? 'tab active' : 'tab'} onClick={() => setTab('slots')}>
          My slots
        </button>
        <button
          className={tab === 'appointments' ? 'tab active' : 'tab'}
          onClick={() => setTab('appointments')}
        >
          Appointments
        </button>
        <button
          className={tab === 'records' ? 'tab active' : 'tab'}
          onClick={() => setTab('records')}
        >
          Medical records
        </button>
      </div>
      {tab === 'slots' ? (
        <SlotsTab doctorId={session?.userId} />
      ) : tab === 'appointments' ? (
        <AppointmentsTab />
      ) : (
        <RecordsTab />
      )}
    </div>
  );
}

function SlotsTab({ doctorId }: { doctorId?: number }) {
  const [date, setDate] = useState(today());
  const [slots, setSlots] = useState<ScheduleResponse[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [start, setStart] = useState('09:00');
  const [end, setEnd] = useState('10:00');
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');

  const load = useCallback(async () => {
    if (!doctorId) return;
    try {
      setSlots(await schedulesApi.byDoctor(doctorId, date));
      setLoaded(true);
      setError('');
    } catch {
      setError('Could not load slots.');
    }
  }, [doctorId, date]);

  useEffect(() => {
    setLoaded(false);
    load();
  }, [load]);

  async function createSlot(e: FormEvent) {
    e.preventDefault();
    try {
      await schedulesApi.create({ availableDate: date, startTime: start, endTime: end });
      setMsg('Slot created.');
      await load();
    } catch {
      setError('Could not create slot. Check the date/time.');
    }
  }

  async function removeSlot(id: number) {
    try {
      await schedulesApi.remove(id);
      setMsg('Slot deleted.');
      await load();
    } catch {
      setError('Could not delete slot. It may be booked.');
    }
  }

  return (
    <div>
      <form className="filters" onSubmit={createSlot}>
        <input type="date" value={date} min={today()} onChange={(e) => setDate(e.target.value)} />
        <input type="time" value={start} onChange={(e) => setStart(e.target.value)} />
        <input type="time" value={end} onChange={(e) => setEnd(e.target.value)} />
        <button className="primary" type="submit">
          Add slot
        </button>
      </form>
      <ErrorBox message={error} />
      {msg && <div className="success">{msg}</div>}
      {!loaded ? (
        <Loading label="Loading slots..." />
      ) : slots.length === 0 ? (
        <p className="muted">No slots for this date.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Start</th>
              <th>End</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {slots.map((s) => (
              <tr key={s.id}>
                <td>{s.availableDate}</td>
                <td>{s.startTime}</td>
                <td>{s.endTime}</td>
                <td>{s.booked ? <span className="badge badge-confirmed">Booked</span> : 'Free'}</td>
                <td>
                  {!s.booked && (
                    <button className="ghost" onClick={() => removeSlot(s.id)}>
                      Delete
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

function AppointmentsTab() {
  const [appointments, setAppointments] = useState<PageResponse<AppointmentResponse> | null>(null);
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      setAppointments(await appointmentsApi.mine({ page, size: 10 }));
      setError('');
    } catch {
      setError('Could not load appointments.');
    }
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  async function act(id: number, action: 'confirm' | 'reject' | 'complete') {
    try {
      if (action === 'confirm') await appointmentsApi.confirm(id);
      else if (action === 'reject') await appointmentsApi.reject(id);
      else await appointmentsApi.complete(id);
      await load();
    } catch {
      setError('Action failed.');
    }
  }

  if (!appointments) return <Loading label="Loading appointments..." />;

  return (
    <div>
      <ErrorBox message={error} />
      {appointments.content.length === 0 ? (
        <p className="muted">No appointments.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Patient</th>
              <th>Time</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {appointments.content.map((a) => (
              <tr key={a.id}>
                <td>{a.patientName}</td>
                <td>{formatDateTime(a.appointmentTime)}</td>
                <td>
                  <StatusBadge status={a.status} />
                </td>
                <td className="actions">
                  {a.status === 'PENDING' && (
                    <>
                      <button className="primary" onClick={() => act(a.id, 'confirm')}>
                        Confirm
                      </button>
                      <button className="ghost" onClick={() => act(a.id, 'reject')}>
                        Reject
                      </button>
                    </>
                  )}
                  {a.status === 'CONFIRMED' && (
                    <button className="primary" onClick={() => act(a.id, 'complete')}>
                      Complete
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <Pagination page={page} totalPages={appointments.totalPages} onChange={setPage} />
    </div>
  );
}

function RecordsTab() {
  const [records, setRecords] = useState<PageResponse<MedicalRecordResponse> | null>(null);
  const [completed, setCompleted] = useState<AppointmentResponse[]>([]);
  const [page, setPage] = useState(0);
  const [appointmentId, setAppointmentId] = useState('');
  const [diagnosis, setDiagnosis] = useState('');
  const [prescription, setPrescription] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');

  const load = useCallback(async () => {
    try {
      setRecords(await medicalRecordsApi.mine({ page, size: 10 }));
      const appts = await appointmentsApi.mine({ status: 'COMPLETED', page: 0, size: 100 });
      setCompleted(appts.content);
      setError('');
    } catch {
      setError('Could not load medical records.');
    }
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  async function create(e: FormEvent) {
    e.preventDefault();
    const appt = completed.find((a) => a.id === Number(appointmentId));
    if (!appt) {
      setError('Choose a completed appointment.');
      return;
    }
    try {
      await medicalRecordsApi.create({
        patientId: appt.patientId,
        appointmentId: appt.id,
        diagnosis,
        prescription: prescription || undefined,
        notes: notes || undefined,
      });
      setMsg('Medical record created.');
      setAppointmentId('');
      setDiagnosis('');
      setPrescription('');
      setNotes('');
      await load();
    } catch {
      setError('Could not create medical record.');
    }
  }

  async function remove(id: number) {
    try {
      await medicalRecordsApi.remove(id);
      setMsg('Medical record deleted.');
      await load();
    } catch {
      setError('Could not delete medical record.');
    }
  }

  return (
    <div>
      <ErrorBox message={error} />
      {msg && <div className="success">{msg}</div>}

      <h2>Create record</h2>
      <form className="filters" onSubmit={create}>
        <select value={appointmentId} onChange={(e) => setAppointmentId(e.target.value)} required>
          <option value="">Completed appointment...</option>
          {completed.map((a) => (
            <option key={a.id} value={a.id}>
              {a.patientName} — {formatDateTime(a.appointmentTime)}
            </option>
          ))}
        </select>
        <input
          placeholder="Diagnosis"
          value={diagnosis}
          onChange={(e) => setDiagnosis(e.target.value)}
          required
        />
        <input
          placeholder="Prescription (optional)"
          value={prescription}
          onChange={(e) => setPrescription(e.target.value)}
        />
        <input placeholder="Notes (optional)" value={notes} onChange={(e) => setNotes(e.target.value)} />
        <button className="primary" type="submit">
          Add record
        </button>
      </form>

      {!records ? (
        <Loading label="Loading medical records..." />
      ) : (
        <>
          {records.content.length === 0 ? (
            <p className="muted">No medical records yet.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Patient</th>
                  <th>Date</th>
                  <th>Diagnosis</th>
                  <th>Prescription</th>
                  <th>Notes</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {records.content.map((r) => (
                  <tr key={r.id}>
                    <td>{r.patientName}</td>
                    <td>{formatDateTime(r.createdAt)}</td>
                    <td>{r.diagnosis}</td>
                    <td>{r.prescription || '-'}</td>
                    <td>{r.notes || '-'}</td>
                    <td className="actions">
                      <button className="ghost" onClick={() => remove(r.id)}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <Pagination page={page} totalPages={records.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  );
}
