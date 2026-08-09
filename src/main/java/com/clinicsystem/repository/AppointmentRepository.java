package com.clinicsystem.repository;

import com.clinicsystem.entity.Appointment;
import com.clinicsystem.entity.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientId(Long patientId);

    Page<Appointment> findByPatientId(Long patientId, Pageable pageable);
    Page<Appointment> findByPatientIdAndStatus(Long patientId, AppointmentStatus status, Pageable pageable);

    Page<Appointment> findByDoctorId(Long doctorId, Pageable pageable);
    Page<Appointment> findByDoctorIdAndStatus(Long doctorId, AppointmentStatus status, Pageable pageable);

    List<Appointment> findByStatusAndReminderSentFalse(AppointmentStatus status);

    long countByStatus(AppointmentStatus status);
    long countBySchedule_AvailableDate(LocalDate date);
    long countBySchedule_AvailableDateGreaterThanEqual(LocalDate date);

    boolean existsByDoctorId(Long doctorId);
    boolean existsByPatientId(Long patientId);
}
