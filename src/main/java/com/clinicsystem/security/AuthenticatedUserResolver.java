package com.clinicsystem.security;

import com.clinicsystem.entity.Doctor;
import com.clinicsystem.entity.Patient;
import com.clinicsystem.entity.User;
import com.clinicsystem.exception.ResourceNotFoundException;
import com.clinicsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserResolver {

    private final UserRepository userRepository;

    public User resolve(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user no longer exists"));
    }

    public Long resolveId(UserDetails principal) {
        return resolve(principal).getId();
    }

    public Patient resolveAsPatient(UserDetails principal) {
        User user = resolve(principal);
        if (!(user instanceof Patient patient)) {
            throw new IllegalStateException("Authenticated user is not a patient");
        }
        return patient;
    }

    public Doctor resolveAsDoctor(UserDetails principal) {
        User user = resolve(principal);
        if (!(user instanceof Doctor doctor)) {
            throw new IllegalStateException("Authenticated user is not a doctor");
        }
        return doctor;
    }
}