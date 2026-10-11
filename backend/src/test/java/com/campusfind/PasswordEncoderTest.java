package com.campusfind;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    @Test
    void testAdminPasswordHashMatches() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        // Verifies the seeded admin password hash matches Admin@123
        assertThat(encoder.matches("Admin@123", "$2a$10$32D3/bzLGBa4ISzZ3n9ifOYoAuuMT9dkVUBkf4R3o/9Uy.CaMMGhG")).isTrue();
    }
}
