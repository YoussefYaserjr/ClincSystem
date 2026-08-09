package com.clinicsystem.controller;

import com.clinicsystem.dto.response.DoctorResponse;
import com.clinicsystem.dto.response.PageResponse;
import com.clinicsystem.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Doctors", description = "Public doctor discovery and search")
public class DoctorController {

    private static final int DEFAULT_SIZE = 20;

    private final DoctorService doctorService;

    @GetMapping("/doctors")
    @Operation(summary = "List all approved doctors", description = "Paginated list of approved doctors, optionally filtered by specialty and/or location.")
    public PageResponse<DoctorResponse> all(
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        if (specialty != null && location != null) {
            return doctorService.searchBySpecialtyAndLocation(specialty, location, page, size);
        } else if (specialty != null) {
            return doctorService.searchBySpecialty(specialty, page, size);
        } else if (location != null) {
            return doctorService.searchByLocation(location, page, size);
        }
        return doctorService.getAll(page, size);
    }

    @GetMapping("/doctors/{id}")
    @Operation(summary = "Get approved doctor by id")
    public DoctorResponse getById(@PathVariable Long id) {
        return doctorService.getById(id);
    }

    @GetMapping("/search/doctors")
    @Operation(summary = "Search approved doctors", description = "Filter by specialty and/or location. No filters returns all approved doctors.")
    public PageResponse<DoctorResponse> search(
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return all(specialty, location, page, size);
    }
}
