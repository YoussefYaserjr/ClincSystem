package com.clinicsystem.service;

import com.clinicsystem.dto.response.DoctorResponse;
import java.util.List;

public interface DoctorService {
    List<DoctorResponse> getAll();
    DoctorResponse getById(Long id);
    List<DoctorResponse> searchBySpecialty(String specialty);
    List<DoctorResponse> searchByLocation(String location);
    List<DoctorResponse> searchBySpecialtyAndLocation(String specialty, String location);
}