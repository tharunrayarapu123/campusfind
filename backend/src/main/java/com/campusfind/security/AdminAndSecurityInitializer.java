package com.campusfind.security;

import com.campusfind.entity.Role;
import com.campusfind.entity.User;
import com.campusfind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAndSecurityInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${app.auth.admin-initial-password:}")
    private String adminInitialPassword;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        userRepository.findByEmail("admin@college.edu").ifPresentOrElse(admin -> {
            if (adminInitialPassword != null && !adminInitialPassword.trim().isEmpty()) {
                admin.setPasswordHash(passwordEncoder.encode(adminInitialPassword.trim()));
                userRepository.save(admin);
                log.info("System administrator password initialized/updated from ADMIN_INITIAL_PASSWORD environment variable.");
            } else if (isProd) {
                // In production profile, if ADMIN_INITIAL_PASSWORD is not explicitly provided,
                // scramble the default dev password so the hardcoded 'Admin@123' cannot be used in prod.
                admin.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                userRepository.save(admin);
                log.warn("CRITICAL SECURITY NOTICE: In 'prod' profile without ADMIN_INITIAL_PASSWORD, default password was scrambled to prevent unauthorized access.");
            }
        }, () -> {
            if (adminInitialPassword != null && !adminInitialPassword.trim().isEmpty()) {
                User newAdmin = User.builder()
                        .fullName("System Administrator")
                        .email("admin@college.edu")
                        .passwordHash(passwordEncoder.encode(adminInitialPassword.trim()))
                        .department("Campus Administration")
                        .role(Role.ADMIN)
                        .karmaPoints(100)
                        .banned(false)
                        .createdAt(LocalDateTime.now())
                        .build();
                userRepository.save(newAdmin);
                log.info("System administrator created successfully from ADMIN_INITIAL_PASSWORD.");
            }
        });
    }
}
