package com.unileste.sisges.controller.dto.classmeeting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassMeetingSearchRequest {

    private LocalDate date;
    private Integer disciplineId;
    private Integer classId;
    private Integer teacherId;
}
