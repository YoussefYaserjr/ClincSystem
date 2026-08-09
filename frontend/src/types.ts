export type Role = 'PATIENT' | 'DOCTOR' | 'ADMIN';

export type AppointmentStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'COMPLETED';

export interface AuthResponse {
  token: string;
  userId: number;
  role: Role;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface DoctorResponse {
  id: number;
  name: string;
  email: string;
  phone: string;
  specialty: string;
  location: string;
  clinic: string;
  experience: number;
  consultationFee: number;
  rating: number;
  approved: boolean;
}

export interface ScheduleResponse {
  id: number;
  doctorId: number;
  availableDate: string;
  startTime: string;
  endTime: string;
  booked: boolean;
}

export interface AppointmentResponse {
  id: number;
  doctorId: number;
  doctorName: string;
  patientId: number;
  patientName: string;
  appointmentTime: string;
  status: AppointmentStatus;
}

export interface UserResponse {
  id: number;
  name: string;
  email: string;
  phone: string;
  role: Role;
}

export interface StatsResponse {
  totalUsers: number;
  totalPatients: number;
  totalDoctors: number;
  pendingDoctors: number;
  totalAppointments: number;
  pendingAppointments: number;
  confirmedAppointments: number;
  completedAppointments: number;
  cancelledAppointments: number;
  rejectedAppointments: number;
  todayAppointments: number;
  upcomingAppointments: number;
  totalSchedules: number;
  availableSlots: number;
  totalMedicalRecords: number;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  phone: string;
  role: 'PATIENT' | 'DOCTOR';
  specialty?: string;
  location?: string;
}
