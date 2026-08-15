package com.clinicsystem.repository;

import com.clinicsystem.entity.Appointment;
import com.clinicsystem.entity.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientId(Long patientId);

    Page<Appointment> findByPatientId(Long patientId, Pageable pageable);
    Page<Appointment> findByPatientIdAndStatus(Long patientId, AppointmentStatus status, Pageable pageable);

    Page<Appointment> findByDoctorId(Long doctorId, Pageable pageable);
    Page<Appointment> findByDoctorIdAndStatus(Long doctorId, AppointmentStatus status, Pageable pageable);

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.status = :status
              AND a.reminderSent = false
              AND (a.schedule.availableDate > :startDate
                   OR (a.schedule.availableDate = :startDate AND a.schedule.startTime >= :startTime))
              AND (a.schedule.availableDate < :endDate
                   OR (a.schedule.availableDate = :endDate AND a.schedule.startTime <= :endTime))
            """)
    List<Appointment> findDueReminders(@Param("status") AppointmentStatus status,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("startTime") LocalTime startTime,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("endTime") LocalTime endTime);

    long countByStatus(AppointmentStatus status);
    long countBySchedule_AvailableDate(LocalDate date);
    long countBySchedule_AvailableDateGreaterThanEqual(LocalDate date);

    boolean existsByDoctorId(Long doctorId);
    boolean existsByPatientId(Long patientId);
}
