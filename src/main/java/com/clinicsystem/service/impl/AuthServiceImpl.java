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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
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

        User user;
        if (req.getRole() == Role.DOCTOR) {
            user = Doctor.builder()
                    .name(req.getName()).email(req.getEmail())
                    .phone(req.getPhone())
                    .password(passwordEncoder.encode(req.getPassword()))
                    .role(Role.DOCTOR)
                    .specialty(req.getSpecialty())
                    .location(req.getLocation())
                    .build();
        } else {
            user = Patient.builder()
                    .name(req.getName()).email(req.getEmail())
                    .phone(req.getPhone())
                    .password(passwordEncoder.encode(req.getPassword()))
                    .role(Role.PATIENT)
                    .bloodType(req.getBloodType())
                    .gender(req.getGender())
                    .build();
        }

        userRepository.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail()).password(user.getPassword())
                .authorities("ROLE_" + user.getRole()).build();

        return AuthResponse.builder()
                .token(jwtService.generateToken(userDetails))
                .userId(user.getId())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail()).password(user.getPassword())
                .authorities("ROLE_" + user.getRole()).build();

        return AuthResponse.builder()
                .token(jwtService.generateToken(userDetails))
                .userId(user.getId())
                .role(user.getRole().name())
                .build();
    }
}