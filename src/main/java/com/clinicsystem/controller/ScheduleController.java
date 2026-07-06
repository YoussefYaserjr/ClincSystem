package com.clinicsystem.controller;

import com.clinicsystem.dto.request.CreateScheduleRequest;
import com.clinicsystem.dto.response.ScheduleResponse;
import com.clinicsystem.security.AuthenticatedUserResolver;
import com.clinicsystem.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final AuthenticatedUserResolver userResolver;

    @PostMapping
    public ResponseEntity<ScheduleResponse> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateScheduleRequest request) {

        Long doctorId = userResolver.resolveAsDoctor(principal).getId();
        return ResponseEntity.ok(scheduleService.create(doctorId, request));
    }

    // Public: patients need to browse a doctor's open slots before booking.
    @GetMapping("/doctor/{doctorId}")
    public List<ScheduleResponse> availableSlots(
            @PathVariable Long doctorId,
            @RequestParam LocalDate date) {
        return scheduleService.getAvailableSlots(doctorId, date);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {

        Long doctorId = userResolver.resolveAsDoctor(principal).getId();
        scheduleService.delete(id, doctorId);
        return ResponseEntity.noContent().build();
    }
}