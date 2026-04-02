package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.material.CreateDisciplineMaterialRequest;
import com.unileste.sisges.controller.dto.material.DisciplineMaterialResponse;
import com.unileste.sisges.exception.BusinessRuleException;
import com.unileste.sisges.exception.ResourceNotFoundException;
import com.unileste.sisges.model.*;
import com.unileste.sisges.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DisciplineMaterialService {

    private final DisciplineMaterialRepository materialRepository;
    private final DisciplineRepository disciplineRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public List<DisciplineMaterialResponse> findByClassAndDiscipline(Integer classId, Integer disciplineId) {
        List<DisciplineMaterial> materials = disciplineId != null
                ? materialRepository.findByDisciplineIdAndSchoolClassIdAndDeletedAtIsNullOrderByCreatedAtDesc(disciplineId, classId)
                : materialRepository.findBySchoolClassIdAndDeletedAtIsNullOrderByCreatedAtDesc(classId);
        return materials.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DisciplineMaterialResponse> findByClassAndDisciplineForTeacher(
            Integer classId, Integer disciplineId, Integer teacherUserId) {
        Teacher teacher = teacherRepository.findByBaseData_IdAndDeletedAtIsNull(teacherUserId)
                .orElseThrow(() -> new BusinessRuleException("Professor não encontrado."));
        List<Integer> allowedDisciplineIds = teacher.getDisciplines().stream()
                .filter(d -> d.getDeletedAt() == null)
                .map(Discipline::getId)
                .filter(Objects::nonNull)
                .toList();
        if (allowedDisciplineIds.isEmpty()) {
            return List.of();
        }
        if (disciplineId != null) {
            if (!allowedDisciplineIds.contains(disciplineId)) {
                return List.of();
            }
            return materialRepository
                    .findByDisciplineIdAndSchoolClassIdAndDeletedAtIsNullOrderByCreatedAtDesc(disciplineId, classId)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }
        return materialRepository
                .findBySchoolClass_IdAndDiscipline_IdInAndDeletedAtIsNullOrderByCreatedAtDesc(classId, allowedDisciplineIds)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DisciplineMaterialResponse> findForStudent(Integer studentUserId) {
        Student student = studentRepository.findByBaseData_IdAndDeletedAtIsNull(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno", studentUserId));
        if (student.getCurrentClass() == null) {
            return List.of();
        }
        return findByClassAndDiscipline(student.getCurrentClass().getId(), null);
    }

    @Transactional
    public DisciplineMaterialResponse create(CreateDisciplineMaterialRequest request, Integer teacherUserId) {
        Teacher teacher = teacherRepository.findByBaseData_IdAndDeletedAtIsNull(teacherUserId)
                .orElseThrow(() -> new BusinessRuleException("Apenas professores podem criar materiais."));

        Discipline discipline = disciplineRepository.findByIdAndDeletedAtIsNull(request.getDisciplineId())
                .orElseThrow(() -> new ResourceNotFoundException("Disciplina", request.getDisciplineId()));
        SchoolClass schoolClass = schoolClassRepository.findByIdAndDeletedAtIsNull(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Turma", request.getClassId()));

        if (!schoolClass.getDisciplines().stream().anyMatch(d -> d.getId().equals(discipline.getId()))) {
            throw new BusinessRuleException("A disciplina não está vinculada à turma.");
        }
        if (!schoolClass.getTeachers().stream().anyMatch(t -> t.getId().equals(teacher.getId()))) {
            throw new BusinessRuleException("O professor não está vinculado à turma.");
        }
        if (!discipline.getTeachers().stream().anyMatch(t -> t.getId().equals(teacher.getId()))) {
            throw new BusinessRuleException("O professor não está vinculado à disciplina.");
        }

        DisciplineMaterial material = DisciplineMaterial.builder()
                .discipline(discipline)
                .schoolClass(schoolClass)
                .teacher(teacher)
                .title(request.getTitle())
                .description(request.getDescription())
                .materialType(request.getMaterialType())
                .filePath(request.getFilePath())
                .build();

        material = materialRepository.save(material);
        return toResponse(material);
    }

    @Transactional
    public void delete(Integer id, Integer teacherUserId) {
        DisciplineMaterial m = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material", id));
        if (m.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Material", id);
        }
        Teacher teacher = teacherRepository.findByBaseData_IdAndDeletedAtIsNull(teacherUserId)
                .orElseThrow(() -> new BusinessRuleException("Apenas professores podem excluir materiais."));
        if (m.getTeacher() == null || !m.getTeacher().getId().equals(teacher.getId())) {
            throw new BusinessRuleException("Apenas o professor que criou pode excluir o material.");
        }
        m.setDeletedAt(java.time.LocalDateTime.now());
        materialRepository.save(m);
    }

    private DisciplineMaterialResponse toResponse(DisciplineMaterial m) {
        return DisciplineMaterialResponse.builder()
                .id(m.getId())
                .disciplineId(m.getDiscipline().getId())
                .disciplineName(m.getDiscipline().getName())
                .classId(m.getSchoolClass() != null ? m.getSchoolClass().getId() : null)
                .className(m.getSchoolClass() != null ? m.getSchoolClass().getName() : null)
                .title(m.getTitle())
                .description(m.getDescription())
                .materialType(m.getMaterialType())
                .filePath(m.getFilePath())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
