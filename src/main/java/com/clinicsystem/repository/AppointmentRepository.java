package com.clinicsystem.repository;

import com.clinicsystem.entity.Appointment;
import com.clinicsystem.entity.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findByDoctorId(Long doctorId);
    List<Appointment> findByStatusAndReminderSentFalse(AppointmentStatus status);
}