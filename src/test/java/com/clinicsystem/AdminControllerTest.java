package com.clinicsystem;

import com.clinicsystem.dto.response.AuthResponse;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTest extends AbstractIntegrationTest {

    @Test
    void unauthenticatedCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminCannotAccessAdminEndpoints() throws Exception {
        AuthResponse patient = registerPatient();

        mockMvc.perform(bearer(get("/admin/stats"), patient.getToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void pendingDoctorIsHiddenFromPublicUntilApproved() throws Exception {
        AuthResponse doctor = register("pending-" + System.nanoTime() + "@test.com",
                "DOCTOR", "Cardiology", "Cairo");

        mockMvc.perform(get("/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + doctor.getUserId() + ")]").doesNotExist());

        mockMvc.perform(bearer(get("/admin/doctors?approved=false"), adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + doctor.getUserId() + ")]").exists());

        mockMvc.perform(bearer(post("/admin/doctors/{id}/approve", doctor.getUserId()), adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(true));

        mockMvc.perform(get("/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + doctor.getUserId() + ")]").exists());
    }

    @Test
    void rejectWithdrawsApproval() throws Exception {
        AuthResponse doctor = register("reject-" + System.nanoTime() + "@test.com",
                "DOCTOR", "Dermatology", "Cairo");
        approveDoctor(doctor.getUserId());

        mockMvc.perform(bearer(post("/admin/doctors/{id}/reject", doctor.getUserId()), adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(false));

        mockMvc.perform(get("/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + doctor.getUserId() + ")]").doesNotExist());
    }

    @Test
    void statsReturnsOverviewCounts() throws Exception {
        mockMvc.perform(bearer(get("/admin/stats"), adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").isNumber())
                .andExpect(jsonPath("$.totalDoctors").isNumber())
                .andExpect(jsonPath("$.totalAppointments").isNumber())
                .andExpect(jsonPath("$.availableSlots").isNumber());
    }

    @Test
    void approveMissingDoctorIsNotFound() throws Exception {
        mockMvc.perform(bearer(post("/admin/doctors/{id}/approve", 999999L), adminToken()))
                .andExpect(status().isNotFound());
    }
}
