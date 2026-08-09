package com.clinicsystem.controller;

import com.clinicsystem.dto.request.BookAppointmentRequest;
import com.clinicsystem.dto.response.AppointmentResponse;
import com.clinicsystem.dto.response.PageResponse;
import com.clinicsystem.entity.Doctor;
import com.clinicsystem.entity.Patient;
import com.clinicsystem.entity.User;
import com.clinicsystem.entity.enums.AppointmentStatus;
import com.clinicsystem.security.AuthenticatedUserResolver;
import com.clinicsystem.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Book, cancel and manage appointment lifecycle")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AuthenticatedUserResolver userResolver;

    @PostMapping
    @Operation(summary = "Book an appointment", description = "Patient books an available slot for a doctor.")
    public ResponseEntity<AppointmentResponse> book(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody BookAppointmentRequest request) {

        Long patientId = userResolver.resolveAsPatient(principal).getId();
        return ResponseEntity.ok(appointmentService.book(patientId, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel an appointment", description = "Patient or doctor cancels an appointment and frees the slot.")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {

        Long requesterId = userResolver.resolveId(principal);
        appointmentService.cancel(id, requesterId);
        return ResponseEntity.noContent().build();
    }

    /** Doctor accepts a PENDING appointment. */
    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm an appointment", description = "Doctor accepts a PENDING appointment.")
    public ResponseEntity<AppointmentResponse> confirm(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {

        Long doctorId = userResolver.resolveAsDoctor(principal).getId();
        return ResponseEntity.ok(appointmentService.confirm(id, doctorId));
    }

    /** Doctor rejects a PENDING appointment and frees the schedule slot. */
    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject an appointment", description = "Doctor rejects a PENDING appointment and frees the slot.")
    public ResponseEntity<AppointmentResponse> reject(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {

        Long doctorId = userResolver.resolveAsDoctor(principal).getId();
        return ResponseEntity.ok(appointmentService.reject(id, doctorId));
    }

    /** Doctor marks a CONFIRMED appointment as completed after the visit. */
    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete an appointment", description = "Doctor marks a CONFIRMED appointment as completed after the visit.")
    public ResponseEntity<AppointmentResponse> complete(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {

        Long doctorId = userResolver.resolveAsDoctor(principal).getId();
        return ResponseEntity.ok(appointmentService.complete(id, doctorId));
    }

    @GetMapping("/me")
    @Operation(summary = "My appointments", description = "Returns the authenticated patient's or doctor's appointments, optionally filtered by status.")
    public ResponseEntity<PageResponse<AppointmentResponse>> myAppointments(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User user = userResolver.resolve(principal);
        PageResponse<AppointmentResponse> result = switch (user) {
            case Patient p -> appointmentService.getForPatient(p.getId(), status, page, size);
            case Doctor d -> appointmentService.getForDoctor(d.getId(), status, page, size);
            default -> new PageResponse<>(List.of(), page, size, 0, 0, true);
        };
        return ResponseEntity.ok(result);
    }
}