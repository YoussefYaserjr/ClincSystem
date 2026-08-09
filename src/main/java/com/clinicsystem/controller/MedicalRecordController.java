package com.clinicsystem.controller;

import com.clinicsystem.dto.request.CreateMedicalRecordRequest;
import com.clinicsystem.dto.response.MedicalRecordResponse;
import com.clinicsystem.dto.response.PageResponse;
import com.clinicsystem.security.AuthenticatedUserResolver;
import com.clinicsystem.service.MedicalRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/medical-records")
@RequiredArgsConstructor
@Tag(name = "Medical Records", description = "Manage patient medical records")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final AuthenticatedUserResolver userResolver;

    @PostMapping
    @Operation(summary = "Create medical record", description = "Doctor creates a medical record for a patient.")
    public ResponseEntity<MedicalRecordResponse> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateMedicalRecordRequest request) {

        Long doctorId = userResolver.resolveAsDoctor(principal).getId();
        return ResponseEntity.ok(medicalRecordService.create(doctorId, request));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get patient records", description = "Patient or authorized doctor reads a patient's records.")
    public ResponseEntity<PageResponse<MedicalRecordResponse>> getForPatient(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long requesterId = userResolver.resolveId(principal);
        return ResponseEntity.ok(medicalRecordService.getForPatient(requesterId, patientId, page, size));
    }

    @GetMapping("/mine")
    @Operation(summary = "My authored records", description = "Doctor lists medical records they created.")
    public ResponseEntity<PageResponse<MedicalRecordResponse>> mine(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long doctorId = userResolver.resolveAsDoctor(principal).getId();
        return ResponseEntity.ok(medicalRecordService.getMineAsDoctor(doctorId, page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete medical record", description = "Authoring doctor deletes a medical record.")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {

        Long doctorId = userResolver.resolveAsDoctor(principal).getId();
        medicalRecordService.delete(id, doctorId);
        return ResponseEntity.noContent().build();
    }
}