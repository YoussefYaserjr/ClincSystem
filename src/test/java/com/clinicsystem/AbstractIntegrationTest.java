package com.clinicsystem;

import com.clinicsystem.dto.response.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MySQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class ContainersConfiguration {
        @Bean
        @ServiceConnection
        MySQLContainer<?> mysqlContainer() {
            return new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("clinic_test")
                    .withUsername("test")
                    .withPassword("test");
        }
    }

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final String PASSWORD = "password123";
    private static final String ADMIN_EMAIL = "admin@clinic.com";
    private static final String ADMIN_PASSWORD = "Admin@12345";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    private String adminToken;

    protected AuthResponse registerDoctor(String specialty) throws Exception {
        AuthResponse doctor = register("doctor-" + SEQ.incrementAndGet() + "@test.com",
                "DOCTOR", specialty, "Cairo");
        approveDoctor(doctor.getUserId());
        return doctor;
    }

    protected AuthResponse registerPatient() throws Exception {
        return register("patient-" + SEQ.incrementAndGet() + "@test.com",
                "PATIENT", null, null);
    }

    protected AuthResponse register(String email, String role,
                                    String specialty, String location) throws Exception {
        java.util.LinkedHashMap<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("name", "Test User");
        body.put("email", email);
        body.put("password", PASSWORD);
        body.put("phone", "01000000000");
        body.put("role", role);
        body.put("specialty", specialty);
        body.put("location", location);

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        var node = objectMapper.readTree(result.getResponse().getContentAsString());
        return AuthResponse.builder()
                .token(node.get("token").asText())
                .refreshToken(node.get("refreshToken").asText())
                .userId(node.get("userId").asLong())
                .role(node.get("role").asText())
                .build();
    }

    protected void loginExpect(String email, String password, int expectedStatus) throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", password))))
                .andExpect(status().is(expectedStatus));
    }

    /** Logs in as the bootstrapped admin (cached for the test class). */
    protected synchronized String adminToken() throws Exception {
        if (adminToken == null) {
            MvcResult result = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "email", ADMIN_EMAIL, "password", ADMIN_PASSWORD))))
                    .andExpect(status().isOk())
                    .andReturn();
            adminToken = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("token").asText();
        }
        return adminToken;
    }

    protected void approveDoctor(Long doctorId) throws Exception {
        mockMvc.perform(bearer(post("/admin/doctors/{id}/approve", doctorId), adminToken()))
                .andExpect(status().isOk());
    }

    protected Long createSlot(String doctorToken, LocalDate date,
                              LocalTime start, LocalTime end) throws Exception {
        MvcResult result = mockMvc.perform(bearer(post("/schedules"), doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "availableDate", date.toString(),
                                "startTime", start.toString(),
                                "endTime", end.toString()))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }

    protected Long bookAppointment(String patientToken, Long scheduleId) throws Exception {
        MvcResult result = mockMvc.perform(bearer(post("/appointments"), patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scheduleId", scheduleId))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }

    protected MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder builder, String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected static LocalDate futureDate() {
        return LocalDate.now().plusDays(7);
    }
}
