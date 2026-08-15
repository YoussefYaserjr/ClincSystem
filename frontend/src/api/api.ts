import http from './http';
import type {
  AppointmentResponse,
  AuthResponse,
  DoctorResponse,
  MedicalRecordResponse,
  PageResponse,
  RegisterRequest,
  ScheduleResponse,
  StatsResponse,
  UserResponse,
} from '../types';

export const authApi = {
  login: (email: string, password: string) =>
    http.post<AuthResponse>('/auth/login', { email, password }).then((r) => r.data),
  register: (data: RegisterRequest) =>
    http.post<AuthResponse>('/auth/register', data).then((r) => r.data),
};

export const doctorsApi = {
  list: (params: { specialty?: string; location?: string; page?: number; size?: number }) =>
    http.get<PageResponse<DoctorResponse>>('/doctors', { params }).then((r) => r.data),
  get: (id: number) =>
    http.get<DoctorResponse>(`/doctors/${id}`).then((r) => r.data),
};

export const schedulesApi = {
  byDoctor: (doctorId: number, date: string) =>
    http.get<ScheduleResponse[]>(`/schedules/doctor/${doctorId}`, { params: { date } }).then((r) => r.data),
  create: (data: { availableDate: string; startTime: string; endTime: string }) =>
    http.post<ScheduleResponse>('/schedules', data).then((r) => r.data),
  remove: (id: number) =>
    http.delete<void>(`/schedules/${id}`).then((r) => r.data),
};

export const appointmentsApi = {
  book: (scheduleId: number, notes?: string) =>
    http.post<AppointmentResponse>('/appointments', { scheduleId, notes }).then((r) => r.data),
  mine: (params: { status?: string; page?: number; size?: number }) =>
    http.get<PageResponse<AppointmentResponse>>('/appointments/me', { params }).then((r) => r.data),
  cancel: (id: number) =>
    http.delete<void>(`/appointments/${id}`).then((r) => r.data),
  confirm: (id: number) =>
    http.post<AppointmentResponse>(`/appointments/${id}/confirm`).then((r) => r.data),
  reject: (id: number) =>
    http.post<AppointmentResponse>(`/appointments/${id}/reject`).then((r) => r.data),
  complete: (id: number) =>
    http.post<AppointmentResponse>(`/appointments/${id}/complete`).then((r) => r.data),
};

export interface CreateMedicalRecordPayload {
  patientId: number;
  appointmentId: number;
  diagnosis: string;
  prescription?: string;
  notes?: string;
}

export const medicalRecordsApi = {
  create: (data: CreateMedicalRecordPayload) =>
    http.post<MedicalRecordResponse>('/medical-records', data).then((r) => r.data),
  forPatient: (patientId: number, params: { page?: number; size?: number }) =>
    http
      .get<PageResponse<MedicalRecordResponse>>(`/medical-records/patient/${patientId}`, { params })
      .then((r) => r.data),
  mine: (params: { page?: number; size?: number }) =>
    http.get<PageResponse<MedicalRecordResponse>>('/medical-records/mine', { params }).then((r) => r.data),
  remove: (id: number) =>
    http.delete<void>(`/medical-records/${id}`).then((r) => r.data),
};

export const adminApi = {
  users: (params: { page?: number; size?: number }) =>
    http.get<PageResponse<UserResponse>>('/admin/users', { params }).then((r) => r.data),
  doctors: (params: { approved?: boolean; page?: number; size?: number }) =>
    http.get<PageResponse<DoctorResponse>>('/admin/doctors', { params }).then((r) => r.data),
  approve: (id: number) =>
    http.post<void>(`/admin/doctors/${id}/approve`).then((r) => r.data),
  reject: (id: number) =>
    http.post<void>(`/admin/doctors/${id}/reject`).then((r) => r.data),
  deleteDoctor: (id: number) =>
    http.delete<void>(`/admin/doctors/${id}`).then((r) => r.data),
  stats: () =>
    http.get<StatsResponse>('/admin/stats').then((r) => r.data),
};
