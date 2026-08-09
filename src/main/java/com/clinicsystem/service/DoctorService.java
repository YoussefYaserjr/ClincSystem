package com.clinicsystem.service;

import com.clinicsystem.dto.response.DoctorResponse;
import com.clinicsystem.dto.response.PageResponse;

public interface DoctorService {
    PageResponse<DoctorResponse> getAll(int page, int size);
    DoctorResponse getById(Long id);
    PageResponse<DoctorResponse> searchBySpecialty(String specialty, int page, int size);
    PageResponse<DoctorResponse> searchByLocation(String location, int page, int size);
    PageResponse<DoctorResponse> searchBySpecialtyAndLocation(String specialty, String location, int page, int size);
}
