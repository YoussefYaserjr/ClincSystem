package com.clinicsystem.controller;

import com.clinicsystem.dto.request.CreateMedicalRecordRequest;
import com.clinicsystem.dto.response.MedicalRecordResponse;
import com.clinicsystem.security.AuthenticatedUserResolver;
import com.clinicsystem.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final AuthenticatedUserResolver userResolver;

    @PostMapping
    public ResponseEntity<MedicalRecordResponse> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateMedicalRecordRequest request) {

        Long doctorId = userResolver.resolveAsDoctor(principal).getId();
        return ResponseEntity.ok(medicalRecordService.create(doctorId, request));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicalRecordResponse>> getForPatient(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long patientId) {

        Long requesterId = userResolver.resolveId(principal);
        return ResponseEntity.ok(medicalRecordService.getForPatient(requesterId, patientId));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<MedicalRecordResponse>> mine(
            @AuthenticationPrincipal UserDetails principal) {

        Long doctorId = userResolver.resolveAsDoctor(principal).getId();
        return ResponseEntity.ok(medicalRecordService.getMineAsDoctor(doctorId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {

        Long doctorId = userResolver.resolveAsDoctor(principal).getId();
        medicalRecordService.delete(id, doctorId);
        return ResponseEntity.noContent().build();
    }
}