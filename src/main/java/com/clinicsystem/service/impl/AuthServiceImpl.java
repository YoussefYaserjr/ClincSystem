package com.clinicsystem.service.impl;

import com.clinicsystem.dto.request.LoginRequest;
import com.clinicsystem.dto.request.RegisterRequest;
import com.clinicsystem.dto.response.AuthResponse;
import com.clinicsystem.entity.Doctor;
import com.clinicsystem.entity.Patient;
import com.clinicsystem.entity.User;
import com.clinicsystem.entity.enums.Role;
import com.clinicsystem.repository.UserRepository;
import com.clinicsystem.security.JwtService;
import com.clinicsystem.service.AuthService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (req.getRole() == null || req.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Role must be PATIENT or DOCTOR");
        }

        User user;
        if (req.getRole() == Role.DOCTOR) {
            if (req.getSpecialty() == null || req.getSpecialty().isBlank()) {
                throw new IllegalArgumentException("specialty is required for doctors");
            }
            if (req.getLocation() == null || req.getLocation().isBlank()) {
                throw new IllegalArgumentException("location is required for doctors");
            }

            user = Doctor.builder()
                    .name(req.getName()).email(req.getEmail())
                    .phone(req.getPhone())
                    .password(passwordEncoder.encode(req.getPassword()))
                    .role(Role.DOCTOR)
                    .specialty(req.getSpecialty().trim())
                    .location(req.getLocation().trim())
                    .build();
        } else if (req.getRole() == Role.PATIENT) {
            user = Patient.builder()
                    .name(req.getName()).email(req.getEmail())
                    .phone(req.getPhone())
                    .password(passwordEncoder.encode(req.getPassword()))
                    .role(Role.PATIENT)
                    .bloodType(req.getBloodType())
                    .gender(req.getGender())
                    .build();
        } else {
            throw new IllegalArgumentException("Role must be PATIENT or DOCTOR");
        }

        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        User user;
        try {
            String email = jwtService.extractUsername(refreshToken);
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        UserDetails userDetails = toUserDetails(user);
        if (!jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = toUserDetails(user);
        return AuthResponse.builder()
                .token(jwtService.generateToken(userDetails))
                .refreshToken(jwtService.generateRefreshToken(userDetails))
                .userId(user.getId())
                .role(user.getRole().name())
                .build();
    }

    private UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail()).password(user.getPassword())
                .authorities("ROLE_" + user.getRole()).build();
    }
}
