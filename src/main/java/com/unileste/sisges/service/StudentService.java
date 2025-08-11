package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.request.CreateStudentDto;
import com.unileste.sisges.controller.dto.request.SearchStudentDto;
import com.unileste.sisges.controller.dto.response.StudentResponseDto;
import com.unileste.sisges.mapper.StudentMapper;
import com.unileste.sisges.model.Student;
import com.unileste.sisges.repository.StudentRepository;
import com.unileste.sisges.specification.StudentSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    public Page<StudentResponseDto> search(SearchStudentDto dto) {
        Specification<Student> spec = StudentSpecification.filterByDto(dto);
        Pageable pageable = PageRequest.of(dto == null ? 0 : dto.getPage(), dto == null ? 20 : dto.getSize());

        return studentRepository.findAll(spec, pageable)
                .map(StudentMapper::toStudentResponseDto);
    }

    public StudentResponseDto create(CreateStudentDto request) {
        Student student = StudentMapper.toStudent(request);
        return StudentMapper.toStudentResponseDto(studentRepository.save(student));
    }
}