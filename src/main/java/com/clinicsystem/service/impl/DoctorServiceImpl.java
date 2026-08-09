package com.clinicsystem.service.impl;

import com.clinicsystem.dto.response.DoctorResponse;
import com.clinicsystem.dto.response.PageResponse;
import com.clinicsystem.entity.Doctor;
import com.clinicsystem.exception.ResourceNotFoundException;
import com.clinicsystem.repository.DoctorRepository;
import com.clinicsystem.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;

    @Override
    public PageResponse<DoctorResponse> getAll(int page, int size) {
        Page<Doctor> doctors = doctorRepository.findByApprovedTrue(pageRequest(page, size));
        return PageResponse.of(doctors, this::toResponse);
    }

    @Override
    public DoctorResponse getById(Long id) {
        Doctor doctor = doctorRepository.findByIdAndApprovedTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + id));
        return toResponse(doctor);
    }

    @Override
    public PageResponse<DoctorResponse> searchBySpecialty(String specialty, int page, int size) {
        Page<Doctor> doctors = doctorRepository
                .findByApprovedTrueAndSpecialtyIgnoreCase(specialty, pageRequest(page, size));
        return PageResponse.of(doctors, this::toResponse);
    }

    @Override
    public PageResponse<DoctorResponse> searchByLocation(String location, int page, int size) {
        Page<Doctor> doctors = doctorRepository
                .findByApprovedTrueAndLocationIgnoreCase(location, pageRequest(page, size));
        return PageResponse.of(doctors, this::toResponse);
    }

    @Override
    public PageResponse<DoctorResponse> searchBySpecialtyAndLocation(String specialty, String location, int page, int size) {
        Page<Doctor> doctors = doctorRepository
                .findByApprovedTrueAndSpecialtyIgnoreCaseAndLocationIgnoreCase(specialty, location, pageRequest(page, size));
        return PageResponse.of(doctors, this::toResponse);
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by("name"));
    }

    private DoctorResponse toResponse(Doctor d) {
        return DoctorResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .email(d.getEmail())
                .phone(d.getPhone())
                .specialty(d.getSpecialty())
                .location(d.getLocation())
                .clinic(d.getClinic())
                .experience(d.getExperience())
                .consultationFee(d.getConsultationFee())
                .rating(d.getRating())
                .approved(d.isApproved())
                .build();
    }
}
