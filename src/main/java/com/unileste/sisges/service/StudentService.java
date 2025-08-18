package com.unileste.sisges.service;

import com.unileste.sisges.constants.Constants;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        student.setCreatedAt(LocalDateTime.now());

        String lastRegister = getLastRegister();

        if (lastRegister == null) {
            student.setRegister(Constants.REGISTER_PREFIX + "1000");
        } else {
            int lastRegisterNumbers = Integer.parseInt(lastRegister.substring(1)) + 1;
            student.setRegister(Constants.REGISTER_PREFIX + lastRegisterNumbers);
        }

        return StudentMapper.toStudentResponseDto(studentRepository.save(student));
    }

    private String getLastRegister() {
        List<Student> students = studentRepository.findAll(Sort.by(Sort.Direction.DESC, "Register"));
        if (students.isEmpty()) {
            return null;
        } else {
            return students.getFirst().getRegister();
        }
    }

    public StudentResponseDto findById(Integer id) {
        Optional<Student> optStudent = studentRepository.findById(id);
        if (optStudent.isEmpty() || optStudent.get().getDeletedAt() != null) {
            return null;
        } else {
            Student student = optStudent.get();
            return StudentMapper.toStudentResponseDto(student);
        }
    }

    public Student delete(Integer id) {
        Optional<Student> optStudent = studentRepository.findById(id);
        if (optStudent.isEmpty() || optStudent.get().getDeletedAt() != null) {
            return null;
        } else {
            Student student = optStudent.get();
            student.setDeletedAt(LocalDateTime.now());
            return studentRepository.save(student);
        }
    }
}