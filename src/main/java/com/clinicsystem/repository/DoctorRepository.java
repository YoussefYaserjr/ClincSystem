package com.clinicsystem.repository;

import com.clinicsystem.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findBySpecialtyIgnoreCase(String specialty);

    List<Doctor> findBySpecialtyIgnoreCaseAndLocationIgnoreCase(String specialty, String location);

    List<Doctor> findByLocationIgnoreCase(String location);
}