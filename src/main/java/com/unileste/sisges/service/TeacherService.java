package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.request.TeacherRequest;
import com.unileste.sisges.controller.dto.response.TeacherResponse;
import com.unileste.sisges.mapper.TeacherMapper;
import com.unileste.sisges.model.Teacher;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
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
        } else {
            Teacher teacher = optTeacher.get();
            return TeacherMapper.toResponse(teacher);
        }
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
}