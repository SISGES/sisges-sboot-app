package com.unileste.sisges.mapper;

import com.unileste.sisges.controller.dto.request.CreateStudentDto;
import com.unileste.sisges.controller.dto.response.StudentResponseDto;
import com.unileste.sisges.model.Student;

public class StudentMapper {

    public static StudentResponseDto toStudentResponseDto(Student request) {
        return StudentResponseDto
                .builder()
                .register(request.getRegister())
                .name(request.getName())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .build();
    }

    public static Student toStudent(CreateStudentDto request) {
        return Student
                .builder()
                .name(request.getName())
                .responsible1Name(request.getResponsible1Name())
                .responsible1Phone(request.getResponsible1Phone())
                .responsible1Email(request.getResponsible1Email())
                .responsible2Name(request.getResponsible2Name())
                .responsible2Phone(request.getResponsible2Phone())
                .responsible2Email(request.getResponsible2Email())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .build();
    }
}