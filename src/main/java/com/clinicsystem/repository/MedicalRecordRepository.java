package com.clinicsystem.repository;

import com.clinicsystem.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatientId(Long patientId);
    List<MedicalRecord> findByDoctorId(Long doctorId);

    // Used for ownership checks before update/delete.
    Optional<MedicalRecord> findByIdAndDoctorId(Long id, Long doctorId);
}