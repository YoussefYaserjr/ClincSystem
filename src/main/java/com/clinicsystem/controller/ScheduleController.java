package com.clinicsystem.controller;

import com.clinicsystem.dto.request.CreateScheduleRequest;
import com.clinicsystem.dto.response.ScheduleResponse;
import com.clinicsystem.security.AuthenticatedUserResolver;
import com.clinicsystem.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Schedules", description = "Doctor availability slots")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final AuthenticatedUserResolver userResolver;

    @PostMapping
    @Operation(summary = "Create schedule slot", description = "Doctor adds an available slot to their schedule.")
    public ResponseEntity<ScheduleResponse> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateScheduleRequest request) {

        Long doctorId = userResolver.resolveAsDoctor(principal).getId();
        return ResponseEntity.ok(scheduleService.create(doctorId, request));
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get available slots", description = "Public endpoint returning a doctor's available slots for a date.")
    public List<ScheduleResponse> availableSlots(
            @PathVariable Long doctorId,
            @RequestParam LocalDate date) {
        return scheduleService.getAvailableSlots(doctorId, date);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete schedule slot", description = "Doctor deletes one of their slots.")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {

        Long doctorId = userResolver.resolveAsDoctor(principal).getId();
        scheduleService.delete(id, doctorId);
        return ResponseEntity.noContent().build();
    }
}