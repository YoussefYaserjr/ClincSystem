package com.clinicsystem;

import com.clinicsystem.dto.response.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DoctorControllerTest extends AbstractIntegrationTest {

    @Test
    void listDoctorsIsPublicAndContainsRegisteredDoctor() throws Exception {
        AuthResponse doctor = registerDoctor("Cardiology");

        mockMvc.perform(get("/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + doctor.getUserId() + ")]")
                        .exists())
                .andExpect(jsonPath("$.content[?(@.id == " + doctor.getUserId() + ")].specialty")
                        .value("Cardiology"));
    }

    @Test
    void getDoctorById() throws Exception {
        AuthResponse doctor = registerDoctor("Dermatology");

        mockMvc.perform(get("/doctors/{id}", doctor.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialty").value("Dermatology"))
                .andExpect(jsonPath("$.location").value("Cairo"));
    }

    @Test
    void getDoctorByIdNotFound() throws Exception {
        mockMvc.perform(get("/doctors/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchBySpecialtyIsCaseInsensitive() throws Exception {
        AuthResponse doctor = registerDoctor("Pediatrics");

        mockMvc.perform(get("/search/doctors")
                        .param("specialty", "pediatrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + doctor.getUserId() + ")]")
                        .exists());
    }

    @Test
    void searchByLocationAndSpecialty() throws Exception {
        AuthResponse doctor = registerDoctor("Neurology");

        mockMvc.perform(get("/search/doctors")
                        .param("specialty", "Neurology")
                        .param("location", "Cairo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + doctor.getUserId() + ")]")
                        .exists());
    }

    @Test
    void searchReturnsEmptyWhenNoMatch() throws Exception {
        mockMvc.perform(get("/search/doctors")
                        .param("specialty", "NonExistentSpecialty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }
}
