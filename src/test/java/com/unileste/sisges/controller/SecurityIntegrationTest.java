package com.unileste.sisges.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unileste.sisges.controller.dto.schoolclass.CreateSchoolClassRequest;
import com.unileste.sisges.support.AbstractIntegrationTest;
import com.unileste.sisges.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void studentCannotCreateSchoolClass() throws Exception {
        var scenario = testDataFactory.seedScenario(testDataFactory.uniqueSuffix());
        String token = testDataFactory.loginToken(mockMvc, scenario.studentUser().getEmail(), TestDataFactory.DEFAULT_PASSWORD);

        CreateSchoolClassRequest request = CreateSchoolClassRequest.builder()
                .name("Turma bloqueada")
                .academicYear("7º ano")
                .build();

        mockMvc.perform(post("/api/classes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotRegisterUsers() throws Exception {
        var scenario = testDataFactory.seedScenario(testDataFactory.uniqueSuffix());
        String token = testDataFactory.loginToken(mockMvc, scenario.teacherUser().getEmail(), TestDataFactory.DEFAULT_PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"password\":\"secret12\",\"birthDate\":\"2010-01-01\",\"gender\":\"MALE\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestToProtectedEndpoint_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/classes/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
