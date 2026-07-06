package com.clinicsystem.service.impl;

import com.clinicsystem.dto.response.DoctorResponse;
import com.clinicsystem.entity.Doctor;
import com.clinicsystem.exception.ResourceNotFoundException;
import com.clinicsystem.repository.DoctorRepository;
import com.clinicsystem.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;

    @Override
    public List<DoctorResponse> getAll() {
        return doctorRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DoctorResponse getById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + id));
        return toResponse(doctor);
    }

    @Override
    public List<DoctorResponse> searchBySpecialty(String specialty) {
        return doctorRepository.findBySpecialtyIgnoreCase(specialty).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DoctorResponse> searchByLocation(String location) {
        return doctorRepository.findByLocationIgnoreCase(location).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DoctorResponse> searchBySpecialtyAndLocation(String specialty, String location) {
        return doctorRepository.findBySpecialtyIgnoreCaseAndLocationIgnoreCase(specialty, location).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
                .build();
    }
}