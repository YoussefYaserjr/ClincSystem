package com.clinicsystem;

import com.clinicsystem.dto.response.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MedicalRecordControllerTest extends AbstractIntegrationTest {

    private record Visit(AuthResponse doctor, AuthResponse patient, Long appointmentId) {}

    private Visit completedVisit() throws Exception {
        AuthResponse doctor = registerDoctor("Cardiology");
        AuthResponse patient = registerPatient();
        Long slotId = createSlot(doctor.getToken(), futureDate(), LocalTime.of(9, 0), LocalTime.of(9, 30));
        Long appointmentId = bookAppointment(patient.getToken(), slotId);
        mockMvc.perform(bearer(post("/appointments/{id}/confirm", appointmentId), doctor.getToken()))
                .andExpect(status().isOk());
        mockMvc.perform(bearer(post("/appointments/{id}/complete", appointmentId), doctor.getToken()))
                .andExpect(status().isOk());
        return new Visit(doctor, patient, appointmentId);
    }

    @Test
    void doctorCreatesRecordForOwnAppointment() throws Exception {
        Visit v = completedVisit();

        mockMvc.perform(bearer(post("/medical-records"), v.doctor().getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", v.patient().getUserId(),
                                "appointmentId", v.appointmentId(),
                                "diagnosis", "Seasonal flu",
                                "prescription", "Paracetamol 500mg"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Seasonal flu"));
    }

    @Test
    void recordWithoutAppointmentIdIsRejected() throws Exception {
        Visit v = completedVisit();

        mockMvc.perform(bearer(post("/medical-records"), v.doctor().getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", v.patient().getUserId(),
                                "diagnosis", "Checkup"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void doctorCannotAttachRecordToAnotherDoctorsAppointment() throws Exception {
        Visit v = completedVisit();
        AuthResponse otherDoctor = registerDoctor("Dermatology");

        mockMvc.perform(bearer(post("/medical-records"), otherDoctor.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", v.patient().getUserId(),
                                "appointmentId", v.appointmentId(),
                                "diagnosis", "Hijacked"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void patientViewsOnlyOwnRecords() throws Exception {
        Visit v = completedVisit();
        mockMvc.perform(bearer(post("/medical-records"), v.doctor().getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", v.patient().getUserId(),
                                "appointmentId", v.appointmentId(),
                                "diagnosis", "Seasonal flu"))))
                .andExpect(status().isOk());

        mockMvc.perform(bearer(get("/medical-records/patient/{id}", v.patient().getUserId()),
                        v.patient().getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void patientCannotViewAnotherPatientsRecords() throws Exception {
        Visit v = completedVisit();
        AuthResponse otherPatient = registerPatient();

        mockMvc.perform(bearer(get("/medical-records/patient/{id}", v.patient().getUserId()),
                        otherPatient.getToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void doctorDeletesOwnRecord() throws Exception {
        Visit v = completedVisit();
        String body = objectMapper.writeValueAsString(Map.of(
                "patientId", v.patient().getUserId(),
                "appointmentId", v.appointmentId(),
                "diagnosis", "Routine checkup"));
        Long recordId = objectMapper.readTree(mockMvc.perform(
                        bearer(post("/medical-records"), v.doctor().getToken())
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isOk()).andReturn()
                        .getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(bearer(delete("/medical-records/{id}", recordId), v.doctor().getToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void doctorCannotDeleteAnotherDoctorsRecord() throws Exception {
        Visit v = completedVisit();
        AuthResponse otherDoctor = registerDoctor("Dermatology");
        String body = objectMapper.writeValueAsString(Map.of(
                "patientId", v.patient().getUserId(),
                "appointmentId", v.appointmentId(),
                "diagnosis", "Routine checkup"));
        Long recordId = objectMapper.readTree(mockMvc.perform(
                        bearer(post("/medical-records"), v.doctor().getToken())
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isOk()).andReturn()
                        .getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(bearer(delete("/medical-records/{id}", recordId), otherDoctor.getToken()))
                .andExpect(status().isNotFound());
    }
}
