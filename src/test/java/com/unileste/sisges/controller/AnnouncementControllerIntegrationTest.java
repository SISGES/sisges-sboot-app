package com.unileste.sisges.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unileste.sisges.controller.dto.announcement.CreateAnnouncementRequest;
import com.unileste.sisges.controller.dto.announcement.CreateCommentRequest;
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

class AnnouncementControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void teacherCreatesAnnouncement_andStudentLikesAndComments() throws Exception {
        var scenario = testDataFactory.seedScenario(testDataFactory.uniqueSuffix());
        String teacherToken = testDataFactory.loginToken(mockMvc, scenario.teacherUser().getEmail(), TestDataFactory.DEFAULT_PASSWORD);
        String studentToken = testDataFactory.loginToken(mockMvc, scenario.studentUser().getEmail(), TestDataFactory.DEFAULT_PASSWORD);

        CreateAnnouncementRequest createRequest = CreateAnnouncementRequest.builder()
                .title("Aviso integração " + scenario.suffix())
                .content("Conteúdo do aviso")
                .type("TEXT")
                .ttlHours(10)
                .build();

        var createResult = mockMvc.perform(post("/api/announcements")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Aviso integração " + scenario.suffix()))
                .andExpect(jsonPath("$.activeUntil").isNotEmpty())
                .andReturn();

        int announcementId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asInt();

        mockMvc.perform(get("/api/announcements/feed")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Aviso integração " + scenario.suffix() + "')]").exists());

        mockMvc.perform(post("/api/announcements/{id}/like", announcementId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true));

        CreateCommentRequest commentRequest = CreateCommentRequest.builder()
                .content("Comentário de teste")
                .build();

        mockMvc.perform(post("/api/announcements/{id}/comments", announcementId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Comentário de teste"));
    }
}
