package com.unileste.sisges.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unileste.sisges.controller.dto.auth.LoginRequest;
import com.unileste.sisges.controller.dto.auth.RegisterUserRequest;
import com.unileste.sisges.support.AbstractIntegrationTest;
import com.unileste.sisges.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void login_returnsToken_whenCredentialsAreValid() throws Exception {
        var suffix = testDataFactory.uniqueSuffix();
        var admin = testDataFactory.createAdmin(suffix);

        LoginRequest request = LoginRequest.builder()
                .email(admin.getEmail())
                .password(TestDataFactory.DEFAULT_PASSWORD)
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void login_returnsUnauthorized_whenCredentialsAreInvalid() throws Exception {
        var suffix = testDataFactory.uniqueSuffix();

        LoginRequest unknownUser = LoginRequest.builder()
                .email("unknown-" + suffix + "@test.sisges.local")
                .password("wrong-password")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void register_createsTeacher_whenCalledByAdmin() throws Exception {
        var suffix = testDataFactory.uniqueSuffix();
        var admin = testDataFactory.createAdmin(suffix);
        String token = testDataFactory.loginToken(mockMvc, admin.getEmail(), TestDataFactory.DEFAULT_PASSWORD);

        RegisterUserRequest request = RegisterUserRequest.builder()
                .name("Novo Professor")
                .password("Secret12!")
                .birthDate(LocalDate.of(1995, 5, 5))
                .gender("MALE")
                .role("TEACHER")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.email").isNotEmpty());
    }

    @Test
    void register_returnsForbidden_whenCalledByTeacher() throws Exception {
        var suffix = testDataFactory.uniqueSuffix();
        var teacher = testDataFactory.createTeacher(suffix);
        String token = testDataFactory.loginToken(mockMvc, teacher.getEmail(), TestDataFactory.DEFAULT_PASSWORD);

        RegisterUserRequest request = RegisterUserRequest.builder()
                .name("Novo Aluno")
                .password("Secret12!")
                .birthDate(LocalDate.of(2010, 1, 1))
                .gender("FEMALE")
                .role("STUDENT")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void validate_returnsOk_withValidToken() throws Exception {
        var suffix = testDataFactory.uniqueSuffix();
        var admin = testDataFactory.createAdmin(suffix);
        String token = testDataFactory.tokenFor(admin);

        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
