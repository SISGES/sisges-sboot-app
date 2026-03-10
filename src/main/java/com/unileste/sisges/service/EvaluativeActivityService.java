package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.activity.CreateEvaluativeActivityRequest;
import com.unileste.sisges.controller.dto.activity.EvaluativeActivityResponse;
import com.unileste.sisges.exception.BusinessRuleException;
import com.unileste.sisges.exception.ResourceNotFoundException;
import com.unileste.sisges.model.*;
import com.unileste.sisges.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluativeActivityService {

    private final EvaluativeActivityRepository activityRepository;
    private final ClassMeetingRepository classMeetingRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<EvaluativeActivityResponse> findByClassMeeting(Integer classMeetingId) {
        return activityRepository.findByClassMeetingIdAndDeletedAtIsNullOrderByCreatedAtDesc(classMeetingId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvaluativeActivityResponse> findForStudent(Integer studentUserId) {
        Student student = studentRepository.findByBaseData_IdAndDeletedAtIsNull(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno", studentUserId));
        if (student.getCurrentClass() == null) {
            return List.of();
        }
        return classMeetingRepository.findBySchoolClassIdAndDeletedAtIsNull(student.getCurrentClass().getId())
                .stream()
                .flatMap(cm -> activityRepository.findByClassMeetingIdAndDeletedAtIsNullOrderByCreatedAtDesc(cm.getId()).stream())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EvaluativeActivityResponse create(CreateEvaluativeActivityRequest request, Integer userId) {
        ClassMeeting meeting = classMeetingRepository.findById(request.getClassMeetingId())
                .orElseThrow(() -> new ResourceNotFoundException("Aula", request.getClassMeetingId()));
        if (meeting.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Aula", request.getClassMeetingId());
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getUserRole());
        if (!isAdmin) {
            Teacher teacher = teacherRepository.findByBaseData_IdAndDeletedAtIsNull(userId)
                    .orElseThrow(() -> new BusinessRuleException("Apenas professores podem criar atividades."));
            if (meeting.getTeacher() == null || !meeting.getTeacher().getId().equals(teacher.getId())) {
                throw new BusinessRuleException("Apenas o professor da aula pode criar atividades.");
            }
        }

        EvaluativeActivity activity = EvaluativeActivity.builder()
                .classMeeting(meeting)
                .title(request.getTitle())
                .description(request.getDescription())
                .filePath(request.getFilePath())
                .build();

        activity = activityRepository.save(activity);
        return toResponse(activity);
    }

    @Transactional
    public void delete(Integer id, Integer userId) {
        EvaluativeActivity a = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade", id));
        if (a.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Atividade", id);
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getUserRole());
        if (!isAdmin) {
            Teacher teacher = teacherRepository.findByBaseData_IdAndDeletedAtIsNull(userId)
                    .orElseThrow(() -> new BusinessRuleException("Apenas professores podem excluir atividades."));
            if (a.getClassMeeting().getTeacher() == null || !a.getClassMeeting().getTeacher().getId().equals(teacher.getId())) {
                throw new BusinessRuleException("Apenas o professor da aula pode excluir a atividade.");
            }
        }
        a.setDeletedAt(java.time.LocalDateTime.now());
        activityRepository.save(a);
    }

    private EvaluativeActivityResponse toResponse(EvaluativeActivity a) {
        return EvaluativeActivityResponse.builder()
                .id(a.getId())
                .classMeetingId(a.getClassMeeting().getId())
                .title(a.getTitle())
                .description(a.getDescription())
                .filePath(a.getFilePath())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
