package com.unileste.sisges.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unileste.sisges.controller.dto.grading.UpdateGradingConfigRequest;
import com.unileste.sisges.support.AbstractIntegrationTest;
import com.unileste.sisges.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GradingConfigControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void adminCanGetAndUpdateGradingConfig() throws Exception {
        var suffix = testDataFactory.uniqueSuffix();
        var admin = testDataFactory.createAdmin(suffix);
        String token = testDataFactory.loginToken(mockMvc, admin.getEmail(), TestDataFactory.DEFAULT_PASSWORD);

        mockMvc.perform(get("/api/grading-config")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearMaxPoints").exists());

        UpdateGradingConfigRequest update = new UpdateGradingConfigRequest(
                33,
                33,
                34,
                11, 11, 11,
                11, 11, 11,
                11, 11, 12
        );

        mockMvc.perform(put("/api/grading-config")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearMinPercentage").value(70.00));
    }
}
