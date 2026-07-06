package com.clinicsystem.controller;

import com.clinicsystem.dto.response.DoctorResponse;
import com.clinicsystem.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping("/doctors")
    public List<DoctorResponse> all() {
        return doctorService.getAll();
    }

    @GetMapping("/doctors/{id}")
    public DoctorResponse getById(@PathVariable Long id) {
        return doctorService.getById(id);
    }

    @GetMapping("/search/doctors")
    public List<DoctorResponse> search(@RequestParam(required = false) String specialty,
                                       @RequestParam(required = false) String location) {
        if (specialty != null && location != null) {
            return doctorService.searchBySpecialtyAndLocation(specialty, location);
        } else if (specialty != null) {
            return doctorService.searchBySpecialty(specialty);
        } else if (location != null) {
            return doctorService.searchByLocation(location);
        }
        return doctorService.getAll();
    }
}