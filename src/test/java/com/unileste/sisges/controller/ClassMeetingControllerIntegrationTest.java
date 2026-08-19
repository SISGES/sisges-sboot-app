package com.unileste.sisges.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unileste.sisges.controller.dto.classmeeting.CreateClassMeetingRequest;
import com.unileste.sisges.controller.dto.classmeeting.FrequencyEntryRequest;
import com.unileste.sisges.controller.dto.classmeeting.FrequencyRequest;
import com.unileste.sisges.support.AbstractIntegrationTest;
import com.unileste.sisges.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClassMeetingControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void createMeeting_andSaveFrequency() throws Exception {
        var scenario = testDataFactory.seedScenario(testDataFactory.uniqueSuffix());
        String token = testDataFactory.loginToken(mockMvc, scenario.teacherUser().getEmail(), TestDataFactory.DEFAULT_PASSWORD);

        CreateClassMeetingRequest createRequest = CreateClassMeetingRequest.builder()
                .classId(scenario.schoolClass().getId())
                .disciplineId(scenario.discipline().getId())
                .date(LocalDate.of(2026, 6, 15))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .build();

        var createResult = mockMvc.perform(post("/api/class")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.className").value(scenario.schoolClass().getName()))
                .andReturn();

        int meetingId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asInt();

        FrequencyRequest frequencyRequest = FrequencyRequest.builder()
                .entries(List.of(FrequencyEntryRequest.builder()
                        .studentId(scenario.student().getId())
                        .status("F")
                        .build()))
                .build();

        mockMvc.perform(post("/api/class/{id}/frequency", meetingId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(frequencyRequest)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/class/{id}", meetingId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(meetingId))
                .andExpect(jsonPath("$.classInfo.students[?(@.id == " + scenario.student().getId() + ")]").exists());
    }
}
