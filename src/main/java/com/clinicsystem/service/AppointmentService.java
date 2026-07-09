package com.clinicsystem.service;

import com.clinicsystem.dto.request.BookAppointmentRequest;
import com.clinicsystem.dto.response.AppointmentResponse;
import java.util.List;

public interface AppointmentService {
    AppointmentResponse book(Long patientId, BookAppointmentRequest request);
    void cancel(Long appointmentId, Long requesterId);
    List<AppointmentResponse> getForPatient(Long patientId);
    List<AppointmentResponse> getForDoctor(Long doctorId);
}