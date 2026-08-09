package com.clinicsystem.repository;

import com.clinicsystem.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // Public discovery only exposes approved doctors.
    Page<Doctor> findByApprovedTrue(Pageable pageable);
    Page<Doctor> findByApprovedTrueAndSpecialtyIgnoreCase(String specialty, Pageable pageable);
    Page<Doctor> findByApprovedTrueAndLocationIgnoreCase(String location, Pageable pageable);
    Page<Doctor> findByApprovedTrueAndSpecialtyIgnoreCaseAndLocationIgnoreCase(String specialty, String location, Pageable pageable);
    Optional<Doctor> findByIdAndApprovedTrue(Long id);

    // Admin views.
    Page<Doctor> findByApproved(boolean approved, Pageable pageable);
    long countByApprovedFalse();
}
