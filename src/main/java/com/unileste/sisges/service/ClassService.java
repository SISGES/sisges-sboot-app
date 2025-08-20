package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.request.CreateClassRequestDto;
import com.unileste.sisges.controller.dto.request.UpdateClassRequestDto;
import com.unileste.sisges.controller.dto.response.ClassResponseDto;
import com.unileste.sisges.mapper.ClassMapper;
import com.unileste.sisges.model.ClassEntity;
import com.unileste.sisges.repository.ClassRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassRepository classRepository;

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