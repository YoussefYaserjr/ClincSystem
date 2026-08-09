import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { appointmentsApi, schedulesApi } from '../api/api';
import { useAuth } from '../auth';
import { ErrorBox, Loading, Pagination, StatusBadge, formatDateTime } from '../components/ui';
import type { AppointmentResponse, PageResponse, ScheduleResponse } from '../types';

type Tab = 'slots' | 'appointments';

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
      </div>
      {tab === 'slots' ? (
        <SlotsTab doctorId={session?.userId} />
      ) : (
        <AppointmentsTab />
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
