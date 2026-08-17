package com.unileste.sisges.controller;

import com.unileste.sisges.support.AbstractIntegrationTest;
import com.unileste.sisges.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void studentCanViewOwnClass() throws Exception {
        var scenario = testDataFactory.seedScenario(testDataFactory.uniqueSuffix());
        String token = testDataFactory.loginToken(mockMvc, scenario.studentUser().getEmail(), TestDataFactory.DEFAULT_PASSWORD);

        mockMvc.perform(get("/api/students/me/turma")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.className").value(scenario.schoolClass().getName()));
    }

    @Test
    void studentCanViewAbsencesByDiscipline() throws Exception {
        var scenario = testDataFactory.seedScenario(testDataFactory.uniqueSuffix());
        String token = testDataFactory.loginToken(mockMvc, scenario.studentUser().getEmail(), TestDataFactory.DEFAULT_PASSWORD);

        mockMvc.perform(get("/api/students/me/faltas-por-disciplina")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
