package com.clinicsystem.config;

import com.clinicsystem.entity.User;
import com.clinicsystem.entity.enums.Role;
import com.clinicsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first ADMIN account on startup if none exists.
 * Configure via ADMIN_EMAIL / ADMIN_PASSWORD / ADMIN_NAME environment variables.
 */
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.password}")
    private String password;

    @Value("${app.admin.name}")
    private String name;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User admin = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);
        log.info("Created initial admin account with email {}", email);
    }
}
