package com.clinicsystem.controller;

import com.clinicsystem.dto.request.BookAppointmentRequest;
import com.clinicsystem.dto.response.AppointmentResponse;
import com.clinicsystem.entity.Doctor;
import com.clinicsystem.entity.Patient;
import com.clinicsystem.entity.User;
import com.clinicsystem.security.AuthenticatedUserResolver;
import com.clinicsystem.service.AppointmentService;
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
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AuthenticatedUserResolver userResolver;

    @PostMapping
    public ResponseEntity<AppointmentResponse> book(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody BookAppointmentRequest request) {

        Long patientId = userResolver.resolveAsPatient(principal).getId();
        return ResponseEntity.ok(appointmentService.book(patientId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {

        Long requesterId = userResolver.resolveId(principal);
        appointmentService.cancel(id, requesterId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<List<AppointmentResponse>> myAppointments(
            @AuthenticationPrincipal UserDetails principal) {

        User user = userResolver.resolve(principal);
        List<AppointmentResponse> result = switch (user) {
            case Patient p -> appointmentService.getForPatient(p.getId());
            case Doctor d -> appointmentService.getForDoctor(d.getId());
            default -> List.of();
        };
        return ResponseEntity.ok(result);
    }
}