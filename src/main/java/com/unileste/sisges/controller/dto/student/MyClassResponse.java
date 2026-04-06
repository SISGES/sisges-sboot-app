package com.unileste.sisges.controller.dto.student;

import com.unileste.sisges.controller.dto.schoolclass.StudentSimpleResponse;
import com.unileste.sisges.controller.dto.schoolclass.TeacherSimpleResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyClassResponse {

    private String className;
    private String academicYear;
    private List<StudentSimpleResponse> classmates;
    private List<TeacherSimpleResponse> teachers;
}
