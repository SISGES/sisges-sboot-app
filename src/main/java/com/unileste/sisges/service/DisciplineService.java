package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.discipline.CreateDisciplineRequest;
import com.unileste.sisges.controller.dto.discipline.DisciplineResponse;
import com.unileste.sisges.exception.BusinessRuleException;
import com.unileste.sisges.exception.ResourceNotFoundException;
import com.unileste.sisges.model.Discipline;
import com.unileste.sisges.repository.DisciplineRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DisciplineService {

    private final DisciplineRepository disciplineRepository;

    @Transactional
    public DisciplineResponse create(CreateDisciplineRequest request) {
        if (disciplineRepository.existsByNameAndDeletedAtIsNull(request.getName().trim())) {
            throw new BusinessRuleException("Já existe uma disciplina com o nome informado.");
        }

        Discipline discipline = Discipline.builder()
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
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

    private DisciplineResponse toDisciplineResponse(Discipline discipline) {
        return DisciplineResponse.builder()
                .id(discipline.getId())
                .name(discipline.getName())
                .description(discipline.getDescription())
                .build();
    }
}
