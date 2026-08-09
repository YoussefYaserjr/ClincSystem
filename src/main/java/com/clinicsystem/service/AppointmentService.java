package com.clinicsystem.service;

import com.clinicsystem.dto.request.BookAppointmentRequest;
import com.clinicsystem.dto.response.AppointmentResponse;
import com.clinicsystem.dto.response.PageResponse;
import com.clinicsystem.entity.enums.AppointmentStatus;

public interface AppointmentService {
    AppointmentResponse book(Long patientId, BookAppointmentRequest request);
    void cancel(Long appointmentId, Long requesterId);
    AppointmentResponse confirm(Long appointmentId, Long doctorId);
    AppointmentResponse reject(Long appointmentId, Long doctorId);
    AppointmentResponse complete(Long appointmentId, Long doctorId);
    PageResponse<AppointmentResponse> getForPatient(Long patientId, AppointmentStatus status, int page, int size);
    PageResponse<AppointmentResponse> getForDoctor(Long doctorId, AppointmentStatus status, int page, int size);
}
