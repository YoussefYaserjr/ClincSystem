import type { AppointmentStatus } from '../types';

export function Loading({ label = 'Loading...' }: { label?: string }) {
  return <p className="muted">{label}</p>;
}

export function ErrorBox({ message }: { message: string }) {
  if (!message) return null;
  return <div className="error">{message}</div>;
}

export function StatusBadge({ status }: { status: AppointmentStatus }) {
  const className = `badge badge-${status.toLowerCase()}`;
  return <span className={className}>{status}</span>;
}

export function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <div className="pagination">
      <button disabled={page <= 0} onClick={() => onChange(page - 1)}>
        Prev
      </button>
      <span>
        Page {page + 1} of {totalPages}
      </span>
      <button disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        Next
      </button>
    </div>
  );
}

export function formatDateTime(value: string): string {
  return value.replace('T', ' ');
}

export function money(value: number | undefined | null): string {
  if (value == null) return '';
  return `$${Number(value).toFixed(2)}`;
}
