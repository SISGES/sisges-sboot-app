package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.request.CreateClassRequestDto;
import com.unileste.sisges.controller.dto.request.SearchClassDto;
import com.unileste.sisges.controller.dto.request.UpdateClassRequestDto;
import com.unileste.sisges.controller.dto.response.ClassResponse;
import com.unileste.sisges.controller.dto.response.DetailedClassResponse;
import com.unileste.sisges.mapper.ClassMapper;
import com.unileste.sisges.model.SchoolClass;
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

    public Page<ClassResponse> search(@Valid SearchClassDto search) {
        Specification<SchoolClass> spec = ClassSpecification.filterByDto(search);
        Pageable pageable = PageRequest.of(search == null ? 0 : search.getPage(), search == null ? 20 : search.getSize());

        return classRepository.findAll(spec, pageable)
                .map(ClassMapper::toResponse);
    }

    public DetailedClassResponse findById(Integer id) {
        Optional<SchoolClass> optClass = classRepository.findById(id);
        return optClass.map(ClassMapper::toDetailedResponse).orElse(null);
    }

    public ClassResponse create(CreateClassRequestDto request) {
        if (classRepository.existsByName(request.getName())) {
            return null;
        }

        return ClassMapper.toResponse(classRepository.save(ClassMapper.toEntity(request)));
    }

    public ClassResponse update(@Valid UpdateClassRequestDto request, Integer id) {
        Optional<SchoolClass> optClass = classRepository.findById(id);
        if (optClass.isEmpty() || (!optClass.get().getName().equalsIgnoreCase(request.getName())
                && classRepository.existsByName(request.getName()))) {
            return null;
        }

        SchoolClass aSchoolClass = optClass.get();
        aSchoolClass.setName(request.getName());
        aSchoolClass.setUpdatedAt(LocalDateTime.now());

        return ClassMapper.toResponse(classRepository.save(aSchoolClass));
    }
}