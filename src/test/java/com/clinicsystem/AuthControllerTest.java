package com.clinicsystem;

import com.clinicsystem.dto.response.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends AbstractIntegrationTest {

    @Test
    void registerPatientReturnsTokenWithRolePatient() throws Exception {
        AuthResponse res = registerPatient();

        assertThat(res.getToken()).isNotBlank();
        assertThat(res.getRole()).isEqualTo("PATIENT");
        assertThat(res.getUserId()).isNotNull();
    }

    @Test
    void registerDoctorReturnsTokenWithRoleDoctor() throws Exception {
        AuthResponse res = registerDoctor("Cardiology");

        assertThat(res.getToken()).isNotBlank();
        assertThat(res.getRole()).isEqualTo("DOCTOR");
    }

    @Test
    void registerRejectsAdminRole() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Admin",
                                "email", "admin@test.com",
                                "password", "password123",
                                "role", "ADMIN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        register("duplicate@test.com", "PATIENT", null, null);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Other",
                                "email", "duplicate@test.com",
                                "password", "password123",
                                "role", "PATIENT"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerValidatesMissingRole() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "No Role",
                                "email", "norole@test.com",
                                "password", "password123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("role")));
    }

    @Test
    void registerRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Weak",
                                "email", "weak@test.com",
                                "password", "123",
                                "role", "PATIENT"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerDoctorWithoutSpecialtyIsRejected() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Doc",
                                "email", "doc@test.com",
                                "password", "password123",
                                "role", "DOCTOR"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsTokenForValidCredentials() throws Exception {
        String email = "login@test.com";
        register(email, "PATIENT", null, null);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("PATIENT"));
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        String email = "badlogin@test.com";
        register(email, "PATIENT", null, null);

        loginExpect(email, "wrong-password", 401);
    }

    @Test
    void loginRejectsUnknownEmail() throws Exception {
        loginExpect("nobody@test.com", "password123", 401);
    }
}
