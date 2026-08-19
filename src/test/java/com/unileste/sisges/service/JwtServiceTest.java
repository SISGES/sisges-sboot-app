package com.unileste.sisges.service;

import com.unileste.sisges.model.User;
import com.unileste.sisges.security.JwtService;
import com.unileste.sisges.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", "test-secret-key-minimum-32-characters-long");
        ReflectionTestUtils.setField(jwtService, "expirationTime", 3_600_000L);
    }

    @Test
    void generateToken_andValidate_forUserPrincipal() {
        User user = User.builder()
                .id(10)
                .name("Test User")
                .email("user@test.sisges.local")
                .register("USR001")
                .password("encoded")
                .userRole("TEACHER")
                .build();
        UserPrincipal principal = new UserPrincipal(user);

        String token = jwtService.generateToken(principal);

        assertNotNull(token);
        assertEquals("user@test.sisges.local", jwtService.extractEmail(token));
        assertTrue(jwtService.isTokenValid(token, principal));
    }

    @Test
    void isTokenValid_returnsFalse_forDifferentUser() {
        User user = User.builder()
                .id(10)
                .name("Test User")
                .email("user@test.sisges.local")
                .register("USR001")
                .password("encoded")
                .userRole("TEACHER")
                .build();
        User other = User.builder()
                .id(11)
                .name("Other")
                .email("other@test.sisges.local")
                .register("USR002")
                .password("encoded")
                .userRole("TEACHER")
                .build();

        String token = jwtService.generateToken(new UserPrincipal(user));

        assertFalse(jwtService.isTokenValid(token, new UserPrincipal(other)));
    }
}
