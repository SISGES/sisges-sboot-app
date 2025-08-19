package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.request.CreateClassRequestDto;
import com.unileste.sisges.controller.dto.response.ClassResponseDto;
import com.unileste.sisges.mapper.ClassMapper;
import com.unileste.sisges.repository.ClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}