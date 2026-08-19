package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.activity.CreateEvaluativeActivityRequest;
import com.unileste.sisges.controller.dto.activity.EvaluativeActivityResponse;
import com.unileste.sisges.controller.dto.activity.ActivityGradebookResponse;
import com.unileste.sisges.controller.dto.activity.FillActivityGradesRequest;
import com.unileste.sisges.exception.BusinessRuleException;
import com.unileste.sisges.exception.ResourceNotFoundException;
import com.unileste.sisges.model.*;
import com.unileste.sisges.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvaluativeActivityService {

    private final EvaluativeActivityRepository activityRepository;
    private final ActivityGradeRepository activityGradeRepository;
    private final ClassMeetingRepository classMeetingRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final GradingConfigService gradingConfigService;
    private final AcademicCycleService academicCycleService;

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
        academicCycleService.assertYearStarted();
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
                .activityType(parseActivityType(request.getActivityType()))
                .trimesterNumber(request.getTrimesterNumber())
                .maxPoints(request.getMaxPoints())
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

    @Transactional(readOnly = true)
    public ActivityGradebookResponse getGradebook(Integer id, Integer userId) {
        EvaluativeActivity activity = requireActivity(id);
        assertCanManage(activity, userId);

        List<Student> students = studentRepository
                .findByCurrentClass_IdAndDeletedAtIsNull(activity.getClassMeeting().getSchoolClass().getId());
        Map<Integer, ActivityGrade> grades = activityGradeRepository
                .findByActivityIdOrderByStudent_IdAsc(id).stream()
                .collect(Collectors.toMap(g -> g.getStudent().getId(), Function.identity(), (a, b) -> b));

        return ActivityGradebookResponse.builder()
                .activityId(activity.getId())
                .classMeetingId(activity.getClassMeeting().getId())
                .title(activity.getTitle())
                .activityType(activity.getActivityType().name())
                .trimesterNumber(activity.getTrimesterNumber())
                .maxPoints(activity.getMaxPoints())
                .released(activity.isReleased())
                .students(students.stream()
                        .map(student -> {
                            ActivityGrade grade = grades.get(student.getId());
                            return ActivityGradebookResponse.StudentGradeLine.builder()
                                    .studentId(student.getId())
                                    .userId(student.getBaseData().getId())
                                    .studentName(student.getBaseData().getName())
                                    .score(grade != null ? grade.getScore() : null)
                                    .build();
                        })
                        .toList())
                .build();
    }

    @Transactional
    public ActivityGradebookResponse saveGrades(
            Integer id,
            FillActivityGradesRequest request,
            Integer userId) {
        EvaluativeActivity activity = requireActivity(id);
        assertCanManage(activity, userId);
        if (activity.isReleased()) {
            throw new BusinessRuleException("As notas desta atividade já foram liberadas.");
        }

        Set<Integer> classStudentIds = studentRepository
                .findByCurrentClass_IdAndDeletedAtIsNull(activity.getClassMeeting().getSchoolClass().getId())
                .stream()
                .map(Student::getId)
                .collect(Collectors.toSet());

        request.getEntries().forEach(entry -> {
            if (!classStudentIds.contains(entry.getStudentId())) {
                throw new BusinessRuleException("Aluno não pertence à turma desta atividade.");
            }
            if (entry.getScore() != null && entry.getScore().compareTo(activity.getMaxPoints()) > 0) {
                throw new BusinessRuleException("Nota não pode ser maior que a pontuação máxima da atividade.");
            }
            Student student = studentRepository.findByIdAndDeletedAtIsNull(entry.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Aluno", entry.getStudentId()));
            ActivityGrade grade = activityGradeRepository
                    .findByActivityIdAndStudentId(id, entry.getStudentId())
                    .orElseGet(() -> ActivityGrade.builder()
                            .activity(activity)
                            .student(student)
                            .build());
            grade.setScore(entry.getScore());
            activityGradeRepository.save(grade);
        });

        return getGradebook(id, userId);
    }

    @Transactional
    public EvaluativeActivityResponse release(Integer id, Integer userId) {
        EvaluativeActivity activity = requireActivity(id);
        assertCanManage(activity, userId);
        if (!activity.isReleased()) {
            activity.setReleased(true);
            activity.setReleasedAt(java.time.LocalDateTime.now());
            activity.setReleasedBy(userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId)));
            activity = activityRepository.save(activity);
        }
        return toResponse(activity);
    }

    private EvaluativeActivity requireActivity(Integer id) {
        EvaluativeActivity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade", id));
        if (activity.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Atividade", id);
        }
        return activity;
    }

    private void assertCanManage(EvaluativeActivity activity, Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
        if ("ADMIN".equalsIgnoreCase(user.getUserRole())) {
            return;
        }
        Teacher teacher = teacherRepository.findByBaseData_IdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessRuleException("Apenas professores podem gerenciar notas."));
        if (activity.getClassMeeting().getTeacher() == null
                || !activity.getClassMeeting().getTeacher().getId().equals(teacher.getId())) {
            throw new BusinessRuleException("Apenas o professor da aula pode gerenciar esta atividade.");
        }
    }

    private ActivityType parseActivityType(String value) {
        try {
            return ActivityType.valueOf(value == null ? "ATIVIDADE" : value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Tipo de atividade inválido.");
        }
    }

    private EvaluativeActivityResponse toResponse(EvaluativeActivity a) {
        return EvaluativeActivityResponse.builder()
                .id(a.getId())
                .classMeetingId(a.getClassMeeting().getId())
                .title(a.getTitle())
                .description(a.getDescription())
                .filePath(a.getFilePath())
                .activityType(a.getActivityType().name())
                .trimesterNumber(a.getTrimesterNumber())
                .maxPoints(a.getMaxPoints())
                .released(a.isReleased())
                .releasedAt(a.getReleasedAt())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
