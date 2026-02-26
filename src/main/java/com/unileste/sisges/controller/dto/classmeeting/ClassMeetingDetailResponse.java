package com.unileste.sisges.controller.dto.classmeeting;

import com.unileste.sisges.controller.dto.schoolclass.StudentSimpleResponse;
import com.unileste.sisges.controller.dto.teacher.TeacherSearchResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassMeetingDetailResponse {

    private Integer id;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private ClassInfo classInfo;
    private TeacherSearchResponse teacher;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassInfo {
        private Integer id;
        private String name;
        private String academicYear;
        private List<StudentSimpleResponse> students;
    }
}
