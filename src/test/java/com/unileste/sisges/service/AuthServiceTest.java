package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.auth.LoginRequest;
import com.unileste.sisges.controller.dto.auth.LoginResponse;
import com.unileste.sisges.repository.SchoolClassRepository;
import com.unileste.sisges.repository.StudentRepository;
import com.unileste.sisges.repository.StudentResponsibleRepository;
import com.unileste.sisges.repository.TeacherRepository;
import com.unileste.sisges.repository.UserRepository;
import com.unileste.sisges.security.JwtService;
import com.unileste.sisges.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private UserRepository userRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private StudentResponsibleRepository studentResponsibleRepository;
    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RegistrationService registrationService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_returnsToken_whenCredentialsAreValid() {
        UserPrincipal principal = new UserPrincipal(com.unileste.sisges.model.User.builder()
                .id(1)
                .name("Admin")
                .email("admin@test.sisges.local")
                .register("ADM001")
                .password("hash")
                .userRole("ADMIN")
                .build());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(principal)).thenReturn("jwt-token");

        LoginResponse response = authService.login(LoginRequest.builder()
                .email("admin@test.sisges.local")
                .password("secret")
                .build());

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("ADMIN", response.getUser().getRole());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_throwsBadCredentials_whenAuthenticationFails() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("invalid"));

        assertThrows(BadCredentialsException.class, () -> authService.login(LoginRequest.builder()
                .email("wrong@test.sisges.local")
                .password("bad")
                .build()));
    }
}
