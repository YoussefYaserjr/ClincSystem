package com.clinicsystem.repository;

import com.clinicsystem.entity.Schedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByDoctorIdAndAvailableDate(Long doctorId, LocalDate date);
    List<Schedule> findByDoctorIdAndAvailableDateAndBookedFalse(Long doctorId, LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Schedule s where s.id = :id")
    Optional<Schedule> findByIdForUpdate(@Param("id") Long id);
}