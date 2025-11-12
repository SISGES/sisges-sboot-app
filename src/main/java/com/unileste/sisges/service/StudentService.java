package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.request.StudentRequest;
import com.unileste.sisges.controller.dto.request.SearchStudentRequest;
import com.unileste.sisges.controller.dto.response.StudentResponse;
import com.unileste.sisges.mapper.StudentMapper;
import com.unileste.sisges.model.SchoolClass;
import com.unileste.sisges.model.Student;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.ClassRepository;
import com.unileste.sisges.repository.StudentRepository;
import com.unileste.sisges.specification.StudentSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final UserService userService;

    public Page<StudentResponse> search(SearchStudentRequest dto) {
        Specification<Student> spec = StudentSpecification.filter(dto);
        Pageable pageable = PageRequest.of(dto == null ? 0 : dto.getPage(), dto == null ? 20 : dto.getSize());

        return studentRepository.findAll(spec, pageable)
                .map(StudentMapper::toResponse);
    }

    public StudentResponse create(StudentRequest request) {
        User user = userService.findById(request.getUserId());
        SchoolClass studentClass = null;
        if (request.getClassId() != null) {
            Optional<SchoolClass> optClassEntity = classRepository.findById(request.getClassId());
            studentClass = optClassEntity.orElse(null);
        }
        if (user == null) return null;
        Student student = Student
                .builder()
                .currentClass(studentClass)
                .baseData(user)
                .build();

        return StudentMapper.toResponse(studentRepository.save(student));
    }

    public StudentResponse findById(Integer id) {
        Optional<Student> optStudent = studentRepository.findById(id);
        if (optStudent.isEmpty() || optStudent.get().getBaseData().getDeletedAt() != null) {
            return null;
        } else {
            Student student = optStudent.get();
            return StudentMapper.toResponse(student);
        }
    }

    public Student delete(Integer id) {
        Optional<Student> optStudent = studentRepository.findById(id);
        if (optStudent.isEmpty() || optStudent.get().getBaseData().getDeletedAt() != null) {
            return null;
        } else {
            Student student = optStudent.get();
            student.getBaseData().setDeletedAt(LocalDateTime.now());
            return studentRepository.save(student);
        }
    }

    public StudentResponse findByUserId(Integer id) {
        Optional<Student> optStudent = studentRepository.findByBaseDataId(id);
        if (optStudent.isEmpty() || optStudent.get().getBaseData().getDeletedAt() != null) {
            return null;
        }

        Student student = optStudent.get();
        return StudentMapper.toResponse(student);
    }
}