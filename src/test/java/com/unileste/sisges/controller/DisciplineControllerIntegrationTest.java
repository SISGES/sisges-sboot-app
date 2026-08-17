package com.unileste.sisges.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unileste.sisges.controller.dto.discipline.CreateDisciplineRequest;
import com.unileste.sisges.support.AbstractIntegrationTest;
import com.unileste.sisges.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DisciplineControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void adminCanCreateAndListDisciplines() throws Exception {
        var suffix = testDataFactory.uniqueSuffix();
        var admin = testDataFactory.createAdmin(suffix);
        String token = testDataFactory.loginToken(mockMvc, admin.getEmail(), TestDataFactory.DEFAULT_PASSWORD);

        CreateDisciplineRequest request = CreateDisciplineRequest.builder()
                .name("História " + suffix)
                .description("Disciplina de história")
                .build();

        mockMvc.perform(post("/api/disciplines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("História " + suffix));

        mockMvc.perform(get("/api/disciplines")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'História " + suffix + "')]").exists());
    }
}
