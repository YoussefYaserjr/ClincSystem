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

class ScheduleControllerTest extends AbstractIntegrationTest {

    @Test
    void doctorCreatesSlot() throws Exception {
        AuthResponse doctor = registerDoctor("Cardiology");

        mockMvc.perform(bearer(post("/schedules"), doctor.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "availableDate", futureDate().toString(),
                                "startTime", "09:00",
                                "endTime", "09:30"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.booked").value(false));
    }

    @Test
    void overlappingSlotIsRejected() throws Exception {
        AuthResponse doctor = registerDoctor("Cardiology");
        createSlot(doctor.getToken(), futureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));

        mockMvc.perform(bearer(post("/schedules"), doctor.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "availableDate", futureDate().toString(),
                                "startTime", "09:30",
                                "endTime", "10:30"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void endTimeBeforeStartTimeIsRejected() throws Exception {
        AuthResponse doctor = registerDoctor("Cardiology");

        mockMvc.perform(bearer(post("/schedules"), doctor.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "availableDate", futureDate().toString(),
                                "startTime", "10:00",
                                "endTime", "09:00"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patientCannotCreateSlot() throws Exception {
        AuthResponse patient = registerPatient();

        mockMvc.perform(bearer(post("/schedules"), patient.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "availableDate", futureDate().toString(),
                                "startTime", "09:00",
                                "endTime", "09:30"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void availableSlotsArePublic() throws Exception {
        AuthResponse doctor = registerDoctor("Cardiology");
        createSlot(doctor.getToken(), futureDate(), LocalTime.of(9, 0), LocalTime.of(9, 30));

        mockMvc.perform(get("/schedules/doctor/{id}", doctor.getUserId())
                        .param("date", futureDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void doctorDeletesOwnSlot() throws Exception {
        AuthResponse doctor = registerDoctor("Cardiology");
        Long slotId = createSlot(doctor.getToken(), futureDate(), LocalTime.of(9, 0), LocalTime.of(9, 30));

        mockMvc.perform(bearer(delete("/schedules/{id}", slotId), doctor.getToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void doctorCannotDeleteOtherDoctorsSlot() throws Exception {
        AuthResponse doctorA = registerDoctor("Cardiology");
        AuthResponse doctorB = registerDoctor("Dermatology");
        Long slotId = createSlot(doctorA.getToken(), futureDate(), LocalTime.of(9, 0), LocalTime.of(9, 30));

        mockMvc.perform(bearer(delete("/schedules/{id}", slotId), doctorB.getToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotDeleteBookedSlot() throws Exception {
        AuthResponse doctor = registerDoctor("Cardiology");
        AuthResponse patient = registerPatient();
        Long slotId = createSlot(doctor.getToken(), futureDate(), LocalTime.of(9, 0), LocalTime.of(9, 30));
        bookAppointment(patient.getToken(), slotId);

        mockMvc.perform(bearer(delete("/schedules/{id}", slotId), doctor.getToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedRequestToProtectedEndpointIsUnauthorized() throws Exception {
        mockMvc.perform(post("/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
