package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.discipline.CreateDisciplineRequest;
import com.unileste.sisges.controller.dto.discipline.DisciplineResponse;
import com.unileste.sisges.controller.dto.discipline.UpdateDisciplineRequest;
import com.unileste.sisges.controller.dto.teacher.TeacherSearchResponse;
import com.unileste.sisges.exception.BusinessRuleException;
import com.unileste.sisges.exception.ResourceNotFoundException;
import com.unileste.sisges.model.Discipline;
import com.unileste.sisges.model.Teacher;
import com.unileste.sisges.repository.DisciplineRepository;
import com.unileste.sisges.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisciplineService {

    private final DisciplineRepository disciplineRepository;
    private final TeacherRepository teacherRepository;

    @Transactional
    public DisciplineResponse create(CreateDisciplineRequest request) {
        if (disciplineRepository.existsByNameAndDeletedAtIsNull(request.getName().trim())) {
            throw new BusinessRuleException("Já existe uma disciplina com o nome informado.");
        }

        Discipline discipline = Discipline.builder()
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .teachers(new ArrayList<>())
                .build();

        Discipline saved = disciplineRepository.save(discipline);
        return toDisciplineResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DisciplineResponse> findAll() {
        return disciplineRepository.findAllByDeletedAtIsNull()
                .stream()
                .map(this::toDisciplineResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DisciplineResponse findById(Integer id) {
        Discipline discipline = disciplineRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disciplina", id));
        return toDisciplineResponse(discipline);
    }

    @Transactional
    public DisciplineResponse update(Integer id, UpdateDisciplineRequest request) {
        Discipline discipline = disciplineRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disciplina", id));

        if (!discipline.getName().equals(request.getName().trim())
                && disciplineRepository.existsByNameAndDeletedAtIsNull(request.getName().trim())) {
            throw new BusinessRuleException("Já existe uma disciplina com o nome informado.");
        }

        discipline.setName(request.getName().trim());
        discipline.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

        if (request.getTeachers() != null) {
            for (var link : request.getTeachers()) {
                Teacher teacher = teacherRepository.findByIdAndDeletedAtIsNull(link.getTeacherId())
                        .orElseThrow(() -> new ResourceNotFoundException("Professor", link.getTeacherId()));

                boolean isLinked = discipline.getTeachers().stream()
                        .anyMatch(t -> t.getId().equals(teacher.getId()));

                if (Boolean.TRUE.equals(link.getVinculado()) && !isLinked) {
                    discipline.getTeachers().add(teacher);
                    teacher.getDisciplines().add(discipline);
                } else if (Boolean.FALSE.equals(link.getVinculado()) && isLinked) {
                    discipline.getTeachers().remove(teacher);
                    teacher.getDisciplines().remove(discipline);
                }
            }
        }

        Discipline saved = disciplineRepository.save(discipline);
        return toDisciplineResponse(saved);
    }

    private DisciplineResponse toDisciplineResponse(Discipline discipline) {
        List<TeacherSearchResponse> teachers = discipline.getTeachers().stream()
                .filter(t -> t.getDeletedAt() == null)
                .map(t -> TeacherSearchResponse.builder()
                        .id(t.getId())
                        .name(t.getBaseData().getName())
                        .email(t.getBaseData().getEmail())
                        .build())
                .toList();

        return DisciplineResponse.builder()
                .id(discipline.getId())
                .name(discipline.getName())
                .description(discipline.getDescription())
                .teachers(teachers)
                .build();
    }
}
