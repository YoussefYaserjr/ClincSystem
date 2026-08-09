import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { appointmentsApi, doctorsApi, schedulesApi } from '../api/api';
import { ErrorBox, Loading, money } from '../components/ui';
import type { DoctorResponse, ScheduleResponse } from '../types';

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export default function DoctorDetail() {
  const { id } = useParams();
  const doctorId = Number(id);
  const [doctor, setDoctor] = useState<DoctorResponse | null>(null);
  const [date, setDate] = useState(today());
  const [slots, setSlots] = useState<ScheduleResponse[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');

  const loadDoctor = useCallback(async () => {
    try {
      setDoctor(await doctorsApi.get(doctorId));
    } catch {
      setError('Could not load doctor.');
    }
  }, [doctorId]);

  const loadSlots = useCallback(async () => {
    try {
      setSlots(await schedulesApi.byDoctor(doctorId, date));
      setLoaded(true);
      setError('');
    } catch {
      setError('Could not load slots.');
    }
  }, [doctorId, date]);

  useEffect(() => {
    loadDoctor();
  }, [loadDoctor]);

  useEffect(() => {
    setLoaded(false);
    loadSlots();
  }, [loadSlots]);

  async function book(slot: ScheduleResponse) {
    try {
      await appointmentsApi.book(slot.id);
      setMsg(`Booked ${slot.availableDate} ${slot.startTime} - ${slot.endTime}.`);
      await loadSlots();
    } catch {
      setError('Booking failed. The slot may already be taken.');
    }
  }

  const available = slots.filter((s) => !s.booked);

  return (
    <div>
      <p>
        <Link to="/patient">&larr; Back to doctors</Link>
      </p>
      {doctor && (
        <>
          <h2>{doctor.name}</h2>
          <p className="muted">
            {doctor.specialty} · {doctor.location}
            {doctor.consultationFee ? ` · ${money(doctor.consultationFee)}` : ''}
          </p>
        </>
      )}
      <ErrorBox message={error} />
      {msg && <div className="success">{msg}</div>}

      <label>
        Date
        <input type="date" value={date} min={today()} onChange={(e) => setDate(e.target.value)} />
      </label>

      {!loaded ? (
        <Loading label="Loading slots..." />
      ) : available.length === 0 ? (
        <p className="muted">No available slots on this date.</p>
      ) : (
        <div className="cards">
          {available.map((s) => (
            <div className="card" key={s.id}>
              <p>
                <strong>
                  {s.startTime} - {s.endTime}
                </strong>
              </p>
              <button className="primary" onClick={() => book(s)}>
                Book
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
