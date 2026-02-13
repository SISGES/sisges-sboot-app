package com.unileste.sisges.controller.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {

    private Integer id;
    private String name;
    private String email;
    private String register;
    private LocalDate birthDate;
    private String gender;
    private SchoolClassSimpleResponse currentClass;
    private List<AttendanceResponse> attendances;
    private List<ResponsibleResponse> responsibles;
}