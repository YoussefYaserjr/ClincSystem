package com.clinicsystem;

import com.clinicsystem.dto.response.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppointmentControllerTest extends AbstractIntegrationTest {

    private record Fixture(AuthResponse doctor, AuthResponse patient, Long slotId) {}

    private Fixture newFixture() throws Exception {
        AuthResponse doctor = registerDoctor("Cardiology");
        AuthResponse patient = registerPatient();
        Long slotId = createSlot(doctor.getToken(), futureDate(), LocalTime.of(9, 0), LocalTime.of(9, 30));
        return new Fixture(doctor, patient, slotId);
    }

    @Test
    void fullLifecyclePendingConfirmComplete() throws Exception {
        Fixture f = newFixture();

        Long appointmentId = bookAppointment(f.patient().getToken(), f.slotId());

        mockMvc.perform(bearer(post("/appointments/{id}/confirm", appointmentId), f.doctor().getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(bearer(post("/appointments/{id}/complete", appointmentId), f.doctor().getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void patientCancelsPendingAppointmentAndSlotIsFreed() throws Exception {
        Fixture f = newFixture();
        Long appointmentId = bookAppointment(f.patient().getToken(), f.slotId());

        mockMvc.perform(bearer(delete("/appointments/{id}", appointmentId), f.patient().getToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/schedules/doctor/{id}", f.doctor().getUserId())
                        .param("date", futureDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + f.slotId() + ")]").exists());
    }

    @Test
    void doctorRejectsAppointmentAndSlotIsFreed() throws Exception {
        Fixture f = newFixture();
        Long appointmentId = bookAppointment(f.patient().getToken(), f.slotId());

        mockMvc.perform(bearer(post("/appointments/{id}/reject", appointmentId), f.doctor().getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/schedules/doctor/{id}", f.doctor().getUserId())
                        .param("date", futureDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + f.slotId() + ")]").exists());
    }

    @Test
    void onlyTheAppointmentDoctorCanConfirm() throws Exception {
        Fixture f = newFixture();
        AuthResponse otherDoctor = registerDoctor("Dermatology");
        Long appointmentId = bookAppointment(f.patient().getToken(), f.slotId());

        mockMvc.perform(bearer(post("/appointments/{id}/confirm", appointmentId), otherDoctor.getToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAppointmentParticipantsCanCancel() throws Exception {
        Fixture f = newFixture();
        AuthResponse stranger = registerPatient();
        Long appointmentId = bookAppointment(f.patient().getToken(), f.slotId());

        mockMvc.perform(bearer(delete("/appointments/{id}", appointmentId), stranger.getToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void doctorCannotBookAnAppointment() throws Exception {
        AuthResponse doctor = registerDoctor("Cardiology");
        AuthResponse patient = registerPatient();
        Long slotId = createSlot(doctor.getToken(), futureDate(), LocalTime.of(9, 0), LocalTime.of(9, 30));

        mockMvc.perform(bearer(post("/appointments"), doctor.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("scheduleId", slotId))))
                .andExpect(status().isForbidden());
    }

    @Test
    void bookingNonExistentSlotIs404() throws Exception {
        AuthResponse patient = registerPatient();

        mockMvc.perform(bearer(post("/appointments"), patient.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("scheduleId", 999999L))))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookingAlreadyBookedSlotIs409() throws Exception {
        Fixture f = newFixture();
        AuthResponse otherPatient = registerPatient();
        bookAppointment(f.patient().getToken(), f.slotId());

        mockMvc.perform(bearer(post("/appointments"), otherPatient.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("scheduleId", f.slotId()))))
                .andExpect(status().isConflict());
    }

    @Test
    void cannotCompleteAPendingAppointment() throws Exception {
        Fixture f = newFixture();
        Long appointmentId = bookAppointment(f.patient().getToken(), f.slotId());

        mockMvc.perform(bearer(post("/appointments/{id}/complete", appointmentId), f.doctor().getToken()))
                .andExpect(status().isConflict());
    }

    @Test
    void myAppointmentsReturnsOwnAppointments() throws Exception {
        Fixture f = newFixture();
        Long appointmentId = bookAppointment(f.patient().getToken(), f.slotId());

        mockMvc.perform(bearer(get("/appointments/me"), f.patient().getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + appointmentId + ")]").exists());

        mockMvc.perform(bearer(get("/appointments/me"), f.doctor().getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + appointmentId + ")]").exists());
    }

    @Test
    void concurrentBookingsOnSameSlotOnlyOneSucceeds() throws Exception {
        Fixture f = newFixture();
        AuthResponse patientA = registerPatient();
        AuthResponse patientB = registerPatient();

        int attempts = 2;
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        List<Thread> threads = IntStream.of(0, 1).mapToObj(i -> new Thread(() -> {
            ready.countDown();
            try {
                go.await(10, TimeUnit.SECONDS);
                String token = (i == 0) ? patientA.getToken() : patientB.getToken();
                MvcResult result = mockMvc.perform(bearer(post("/appointments"), token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("scheduleId", f.slotId()))))
                        .andReturn();
                int status = result.getResponse().getStatus();
                if (status == 200) ok.incrementAndGet();
                else if (status == 409) conflict.incrementAndGet();
                else other.incrementAndGet();
            } catch (Exception e) {
                failures.incrementAndGet();
            }
        })).collect(Collectors.toList());

        threads.forEach(Thread::start);
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        go.countDown();
        for (Thread t : threads) {
            t.join(TimeUnit.SECONDS.toMillis(30));
        }

        assertThat(failures.get()).isZero();
        assertThat(other.get()).isZero();
        assertThat(ok.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(1);
    }
}
