package com.clinicsystem.repository;

import com.clinicsystem.entity.MedicalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatientId(Long patientId);
    List<MedicalRecord> findByDoctorId(Long doctorId);

    Page<MedicalRecord> findByPatientId(Long patientId, Pageable pageable);
    Page<MedicalRecord> findByDoctorId(Long doctorId, Pageable pageable);

    // Used for ownership checks before update/delete.
    Optional<MedicalRecord> findByIdAndDoctorId(Long id, Long doctorId);

    boolean existsByDoctorId(Long doctorId);
}
