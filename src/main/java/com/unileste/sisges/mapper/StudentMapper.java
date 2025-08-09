package com.unileste.sisges.mapper;

import com.unileste.sisges.controller.dto.response.StudentResponseDto;
import com.unileste.sisges.model.Student;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StudentMapper {

    StudentResponseDto toDTO(Student student);
}