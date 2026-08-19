package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.event.*;
import com.unileste.sisges.exception.ResourceNotFoundException;
import com.unileste.sisges.model.*;
import com.unileste.sisges.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class SchoolEventService {
    private final SchoolEventRepository eventRepository;
    private final UserRepository userRepository;
    private final SchoolClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    @Transactional(readOnly = true)
    public List<SchoolEventResponse> upcoming(Integer userId, String role) {
        Set<Integer> classIds = "STUDENT".equals(role)
                ? studentRepository.findByBaseData_IdAndDeletedAtIsNull(userId)
                    .map(Student::getCurrentClass).map(SchoolClass::getId).map(Set::of).orElse(Set.of())
                : "TEACHER".equals(role)
                    ? teacherRepository.findByBaseData_IdAndDeletedAtIsNull(userId)
                        .map(Teacher::getClasses).orElse(List.of()).stream()
                        .map(SchoolClass::getId).collect(Collectors.toSet())
                    : Set.of();
        return eventRepository.findByEventAtGreaterThanEqualOrderByEventAtAsc(LocalDateTime.now()).stream()
                .filter(e -> visible(e, role, classIds)).map(e -> toResponse(e, "ADMIN".equals(role))).toList();
    }

    @Transactional
    public SchoolEventResponse create(CreateSchoolEventRequest request, Integer userId) {
        if ("CLASS".equals(request.getAudience()) && request.getClassId() == null)
            throw new IllegalArgumentException("Turma é obrigatória");
        User creator = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
        SchoolClass schoolClass = request.getClassId() == null ? null
                : classRepository.findByIdAndDeletedAtIsNull(request.getClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Turma", request.getClassId()));
        return toResponse(eventRepository.save(SchoolEvent.builder()
                .title(request.getTitle().trim()).description(request.getDescription())
                .eventAt(request.getEventAt()).audience(request.getAudience())
                .schoolClass(schoolClass).createdBy(creator).build()), true);
    }

    @Transactional public void delete(Integer id) { eventRepository.deleteById(id); }

    private boolean visible(SchoolEvent e, String role, Set<Integer> classIds) {
        if ("ADMIN".equals(role) || "ALL".equals(e.getAudience())) return true;
        if ("TEACHERS".equals(e.getAudience())) return "TEACHER".equals(role);
        return "CLASS".equals(e.getAudience()) && e.getSchoolClass() != null
                && classIds.contains(e.getSchoolClass().getId());
    }

    private SchoolEventResponse toResponse(SchoolEvent e, boolean includeCreator) {
        return SchoolEventResponse.builder().id(e.getId()).title(e.getTitle())
                .description(e.getDescription()).eventAt(e.getEventAt()).audience(e.getAudience())
                .classId(e.getSchoolClass() == null ? null : e.getSchoolClass().getId())
                .className(e.getSchoolClass() == null ? null : e.getSchoolClass().getName())
                .createdById(includeCreator ? e.getCreatedBy().getId() : null)
                .createdByName(includeCreator ? e.getCreatedBy().getName() : null)
                .createdAt(e.getCreatedAt()).build();
    }
}
