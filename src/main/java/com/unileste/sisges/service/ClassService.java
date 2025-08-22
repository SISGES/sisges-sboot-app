package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.request.CreateClassRequestDto;
import com.unileste.sisges.controller.dto.request.SearchClassDto;
import com.unileste.sisges.controller.dto.request.UpdateClassRequestDto;
import com.unileste.sisges.controller.dto.response.ClassResponseDto;
import com.unileste.sisges.controller.dto.response.DetailedClassResponseDto;
import com.unileste.sisges.mapper.ClassMapper;
import com.unileste.sisges.model.ClassEntity;
import com.unileste.sisges.repository.ClassRepository;
import com.unileste.sisges.specification.ClassSpecification;
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
public class ClassService {

    private final ClassRepository classRepository;

    public Page<ClassResponseDto> search(@Valid SearchClassDto search) {
        Specification<ClassEntity> spec = ClassSpecification.filterByDto(search);
        Pageable pageable = PageRequest.of(search == null ? 0 : search.getPage(), search == null ? 20 : search.getSize());

        return classRepository.findAll(spec, pageable)
                .map(ClassMapper::toResponse);
    }

    public DetailedClassResponseDto findById(Integer id) {
        Optional<ClassEntity> optClass = classRepository.findById(id);
        return optClass.map(ClassMapper::toDetailedResponse).orElse(null);
    }

    public ClassResponseDto create(CreateClassRequestDto request) {
        if (classRepository.existsByName(request.getName())) {
            return null;
        }

        return ClassMapper.toResponse(classRepository.save(ClassMapper.toEntity(request)));
    }

    public ClassResponseDto update(@Valid UpdateClassRequestDto request, Integer id) {
        Optional<ClassEntity> optClass = classRepository.findById(id);
        if (optClass.isEmpty() || (!optClass.get().getName().equalsIgnoreCase(request.getName())
                && classRepository.existsByName(request.getName()))) {
            return null;
        }

        ClassEntity classEntity = optClass.get();
        classEntity.setName(request.getName());
        classEntity.setUpdatedAt(LocalDateTime.now());

        return ClassMapper.toResponse(classRepository.save(classEntity));
    }
}