package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.request.CreateClassRequestDto;
import com.unileste.sisges.controller.dto.request.SearchClassRequest;
import com.unileste.sisges.controller.dto.request.UpdateClassRequestDto;
import com.unileste.sisges.controller.dto.response.SchoolClassResponse;
import com.unileste.sisges.controller.dto.response.DetailedSchoolClassResponse;
import com.unileste.sisges.controller.dto.response.SchoolClassSimpleResponse;
import com.unileste.sisges.mapper.ClassMapper;
import com.unileste.sisges.model.SchoolClass;
import com.unileste.sisges.model.Student;
import com.unileste.sisges.model.Teacher;
import com.unileste.sisges.repository.ClassRepository;
import com.unileste.sisges.repository.StudentRepository;
import com.unileste.sisges.repository.TeacherRepository;
import com.unileste.sisges.specification.ClassSpecification;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
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
public class SchoolClassService {

    private final ClassRepository schoolClassRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    public Page<SchoolClassResponse> search(@Valid SearchClassRequest search) {
        Specification<SchoolClass> spec = ClassSpecification.filterByDto(search);
        Pageable pageable = PageRequest.of(search == null ? 0 : search.getPage(), search == null ? 20 : search.getSize());

        return schoolClassRepository.findAll(spec, pageable)
                .map(ClassMapper::toResponse);
    }

    public DetailedSchoolClassResponse superFindById(Integer id) {
        Optional<SchoolClass> optClass = schoolClassRepository.findById(id);
        return optClass.map(ClassMapper::toDetailedResponse).orElse(null);
    }

    public SchoolClassResponse create(CreateClassRequestDto request) {
        if (schoolClassRepository.existsByName(request.getName())) {
            return null;
        }

        return ClassMapper.toResponse(schoolClassRepository.save(ClassMapper.toEntity(request)));
    }

    public SchoolClassResponse update(@Valid UpdateClassRequestDto request, Integer id) {
        Optional<SchoolClass> optClass = schoolClassRepository.findById(id);
        if (optClass.isEmpty() || (!optClass.get().getName().equalsIgnoreCase(request.getName())
                && schoolClassRepository.existsByName(request.getName()))) {
            return null;
        }

        SchoolClass aSchoolClass = optClass.get();
        aSchoolClass.setName(request.getName());
        aSchoolClass.setUpdatedAt(LocalDateTime.now());

        return ClassMapper.toResponse(schoolClassRepository.save(aSchoolClass));
    }

    @Transactional
    public SchoolClassResponse bindTeacher(Integer classId, Integer teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
        SchoolClass schoolClass = schoolClassRepository.findById(classId).orElse(null);

        if (teacher == null || schoolClass == null) return null;

        teacher.addClass(schoolClass);
        schoolClass.addTeacher(teacher);

        teacherRepository.save(teacher);

        return ClassMapper.toResponse(schoolClass);
    }

    @Transactional
    public SchoolClassResponse unbindTeacher(Integer classId, Integer teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
        SchoolClass schoolClass = schoolClassRepository.findById(classId).orElse(null);

        if (teacher == null || schoolClass == null) return null;

        teacher.removeClass(schoolClass);
        teacherRepository.save(teacher);

        return ClassMapper.toResponse(schoolClass);
    }

    @Transactional
    public SchoolClassResponse bindStudent(Integer classId, Integer studentId) {
        Student student = studentRepository.findById(studentId).orElse(null);
        SchoolClass schoolClass = schoolClassRepository.findById(classId).orElse(null);

        if (student == null || schoolClass == null) return null;

        student.setCurrentClass(schoolClass);
        schoolClass.addStudent(student);

        studentRepository.save(student);

        return ClassMapper.toResponse(schoolClass);
    }

    @Transactional
    public SchoolClassResponse unbindStudent(Integer classId, Integer studentId) {
        Student student = studentRepository.findById(studentId).orElse(null);
        SchoolClass schoolClass = schoolClassRepository.findById(classId).orElse(null);
        if (student == null || schoolClass == null) return null;

        student.setCurrentClass(null);
        studentRepository.save(student);

        return ClassMapper.toResponse(schoolClass);
    }

    public SchoolClassSimpleResponse findById(Integer id) {
        SchoolClass schoolClass = schoolClassRepository.findById(id).orElse(null);
        if (schoolClass == null) return null;

        return ClassMapper.toSimpleResponse(schoolClass);
    }
}