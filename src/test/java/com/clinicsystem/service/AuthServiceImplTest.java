package com.clinicsystem.service;

import com.clinicsystem.dto.request.LoginRequest;
import com.clinicsystem.dto.request.RegisterRequest;
import com.clinicsystem.dto.response.AuthResponse;
import com.clinicsystem.entity.Doctor;
import com.clinicsystem.entity.Patient;
import com.clinicsystem.entity.User;
import com.clinicsystem.entity.enums.Role;
import com.clinicsystem.repository.UserRepository;
import com.clinicsystem.security.JwtService;
import com.clinicsystem.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthenticationManager authenticationManager;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        authenticationManager = mock(AuthenticationManager.class);
        service = new AuthServiceImpl(userRepository, passwordEncoder, jwtService, authenticationManager);
    }

    private RegisterRequest patientRequest(String email) {
        RegisterRequest req = new RegisterRequest();
        req.setName("Patient One");
        req.setEmail(email);
        req.setPassword("password123");
        req.setPhone("01000000000");
        req.setRole(Role.PATIENT);
        return req;
    }

    private RegisterRequest doctorRequest(String email) {
        RegisterRequest req = patientRequest(email);
        req.setRole(Role.DOCTOR);
        req.setSpecialty("Cardiology");
        req.setLocation("Cairo");
        return req;
    }

    @Test
    void registerPatientPersistsPatientAndReturnsToken() {
        when(userRepository.existsByEmail("p@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(3L);
            return saved;
        });
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");

        AuthResponse response = service.register(patientRequest("p@test.com"));

        assertThat(response.getRole()).isEqualTo("PATIENT");
        assertThat(response.getUserId()).isEqualTo(3L);
        assertThat(response.getToken()).isEqualTo("jwt-token");
        verify(userRepository).save(argThat(user -> user instanceof Patient));
    }

    @Test
    void registerDoctorPersistsDoctor() {
        when(userRepository.existsByEmail("d@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        AuthResponse response = service.register(doctorRequest("d@test.com"));

        assertThat(response.getRole()).isEqualTo("DOCTOR");
        verify(userRepository).save(argThat(user ->
                user instanceof Doctor doctor
                        && "Cardiology".equals(doctor.getSpecialty())
                        && "Cairo".equals(doctor.getLocation())));
    }

    @Test
    void registerDoctorWithoutSpecialtyIsRejected() {
        RegisterRequest req = doctorRequest("d@test.com");
        req.setSpecialty("  ");

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerDuplicateEmailIsRejected() {
        when(userRepository.existsByEmail("p@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(patientRequest("p@test.com")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerAdminRoleIsRejected() {
        RegisterRequest req = patientRequest("admin@test.com");
        req.setRole(Role.ADMIN);

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = Patient.builder().id(3L).email("p@test.com").password("hashed").role(Role.PATIENT).build();
        when(userRepository.findByEmail("p@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");

        LoginRequest request = new LoginRequest();
        request.setEmail("p@test.com");
        request.setPassword("password123");

        AuthResponse response = service.login(request);

        assertThat(response.getRole()).isEqualTo("PATIENT");
        assertThat(response.getToken()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void loginWithBadCredentialsThrows() {
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        LoginRequest request = new LoginRequest();
        request.setEmail("nobody@test.com");
        request.setPassword("password123");

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
