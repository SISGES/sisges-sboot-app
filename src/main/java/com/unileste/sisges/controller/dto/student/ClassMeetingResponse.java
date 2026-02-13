package com.unileste.sisges.controller.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassMeetingResponse {

    private String disciplineName;
    private LocalDate meetingDate;
    private LocalDateTime createdAt;
}