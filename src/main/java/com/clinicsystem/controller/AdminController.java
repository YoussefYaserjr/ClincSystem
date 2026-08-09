package com.clinicsystem.controller;

import com.clinicsystem.dto.response.DoctorResponse;
import com.clinicsystem.dto.response.PageResponse;
import com.clinicsystem.dto.response.StatsResponse;
import com.clinicsystem.dto.response.UserResponse;
import com.clinicsystem.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-only: user management, doctor approval and oversight statistics")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "List all users", description = "Paginated list of every registered user.")
    public PageResponse<UserResponse> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.listUsers(page, size);
    }

    @GetMapping("/doctors")
    @Operation(summary = "List doctors by approval state", description = "approved=true returns approved doctors, approved=false returns pending doctors.")
    public PageResponse<DoctorResponse> listDoctors(
            @RequestParam(defaultValue = "false") boolean approved,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.listDoctors(approved, page, size);
    }

    @PostMapping("/doctors/{id}/approve")
    @Operation(summary = "Approve a doctor", description = "Makes the doctor visible in public discovery and bookable.")
    public ResponseEntity<DoctorResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.approve(id));
    }

    @PostMapping("/doctors/{id}/reject")
    @Operation(summary = "Reject a doctor", description = "Withdraws approval and hides the doctor from public discovery.")
    public ResponseEntity<DoctorResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.reject(id));
    }

    @DeleteMapping("/doctors/{id}")
    @Operation(summary = "Delete a doctor", description = "Permanently removes a doctor that has no appointments, schedules or medical records.")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long id) {
        adminService.deleteDoctor(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Oversight statistics", description = "High-level counts across the system.")
    public StatsResponse stats() {
        return adminService.stats();
    }
}
