package com.clinicsystem.controller;

import com.clinicsystem.dto.response.DoctorResponse;
import com.clinicsystem.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Doctors", description = "Public doctor discovery and search")
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping("/doctors")
    @Operation(summary = "List all doctors")
    public List<DoctorResponse> all() {
        return doctorService.getAll();
    }

    @GetMapping("/doctors/{id}")
    @Operation(summary = "Get doctor by id")
    public DoctorResponse getById(@PathVariable Long id) {
        return doctorService.getById(id);
    }

    @GetMapping("/search/doctors")
    @Operation(summary = "Search doctors", description = "Filter by specialty and/or location. No filters returns all doctors.")
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