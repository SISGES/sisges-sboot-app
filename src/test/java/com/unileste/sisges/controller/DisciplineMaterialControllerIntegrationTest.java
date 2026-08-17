package com.unileste.sisges.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unileste.sisges.controller.dto.material.CreateDisciplineMaterialRequest;
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

class DisciplineMaterialControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void teacherCanCreateAndListMaterial() throws Exception {
        var scenario = testDataFactory.seedScenario(testDataFactory.uniqueSuffix());
        String token = testDataFactory.loginToken(mockMvc, scenario.teacherUser().getEmail(), TestDataFactory.DEFAULT_PASSWORD);

        CreateDisciplineMaterialRequest request = CreateDisciplineMaterialRequest.builder()
                .classId(scenario.schoolClass().getId())
                .disciplineId(scenario.discipline().getId())
                .title("Apostila " + scenario.suffix())
                .description("Material de apoio")
                .materialType("PDF")
                .filePath("/uploads/test.pdf")
                .build();

        mockMvc.perform(post("/api/materials")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Apostila " + scenario.suffix()));

        mockMvc.perform(get("/api/materials")
                        .param("classId", scenario.schoolClass().getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Apostila " + scenario.suffix() + "')]").exists());
    }
}
