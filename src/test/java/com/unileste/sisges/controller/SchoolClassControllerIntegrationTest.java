package com.unileste.sisges.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unileste.sisges.controller.dto.schoolclass.CreateSchoolClassRequest;
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

class SchoolClassControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void createSchoolClass_persistsInDatabase() throws Exception {
        var suffix = testDataFactory.uniqueSuffix();
        var admin = testDataFactory.createAdmin(suffix);
        String token = testDataFactory.loginToken(mockMvc, admin.getEmail(), TestDataFactory.DEFAULT_PASSWORD);

        CreateSchoolClassRequest request = CreateSchoolClassRequest.builder()
                .name("Turma Integração " + suffix)
                .academicYear("8º ano")
                .build();

        var result = mockMvc.perform(post("/api/classes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Turma Integração " + suffix))
                .andReturn();

        int classId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asInt();

        mockMvc.perform(get("/api/classes/" + classId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.academicYear").value("8º ano"));
    }

    @Test
    void addStudent_linksStudentToClass() throws Exception {
        var scenario = testDataFactory.seedScenario(testDataFactory.uniqueSuffix());
        String token = testDataFactory.loginToken(mockMvc, scenario.admin().getEmail(), TestDataFactory.DEFAULT_PASSWORD);

        var extraStudentUser = testDataFactory.createStudent("extra" + scenario.suffix(), null);
        var extraStudent = testDataFactory.findStudentByUserId(extraStudentUser.getId());

        mockMvc.perform(post("/api/classes/{classId}/student/add/{studentId}", scenario.schoolClass().getId(), extraStudent.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students[?(@.email == '" + extraStudentUser.getEmail() + "')]").exists());
    }
}
