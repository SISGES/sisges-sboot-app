package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.request.SearchTeacherRequest;
import com.unileste.sisges.controller.dto.request.TeacherRequest;
import com.unileste.sisges.controller.dto.response.TeacherResponse;
import com.unileste.sisges.mapper.TeacherMapper;
import com.unileste.sisges.model.Teacher;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.TeacherRepository;
import com.unileste.sisges.specification.TeacherSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserService userService;


    public TeacherResponse findById(Integer id) {
        Optional<Teacher> optTeacher = teacherRepository.findById(id);

        if (optTeacher.isEmpty() || optTeacher.get().getBaseData().getDeletedAt() != null) {
            return null;
        }

        Teacher teacher = optTeacher.get();
        return TeacherMapper.toResponse(teacher);
    }

    public TeacherResponse findByUserId(Integer id) {
        Optional<Teacher> optTeacher = teacherRepository.findByBaseDataId(id);
        if (optTeacher.isEmpty() || optTeacher.get().getBaseData().getDeletedAt() != null) {
            return null;
        }

        Teacher teacher = optTeacher.get();
        return TeacherMapper.toResponse(teacher);
    }

    public TeacherResponse create(TeacherRequest request) {
        User user = userService.findById(request.getUserId());
        if (user == null) return null;
        Teacher teacher = Teacher
                .builder()
                .baseData(user)
                .build();

        return TeacherMapper.toResponse(teacherRepository.save(teacher));
    }

    public Page<TeacherResponse> search(SearchTeacherRequest search) {
        Specification<Teacher> spec = TeacherSpecification.filter(search);
        Pageable pageable = PageRequest.of(search == null ? 0 : search.getPage(), search == null ? 20 : search.getSize());

        return teacherRepository.findAll(spec, pageable)
                .map(TeacherMapper::toResponse);
    }
}