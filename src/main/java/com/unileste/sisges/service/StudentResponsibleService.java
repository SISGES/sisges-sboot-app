package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.response.StudentResponsibleResponse;
import com.unileste.sisges.mapper.StudentResponsibleMapper;
import com.unileste.sisges.repository.StudentResponsibleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StudentResponsibleService {

    private final StudentResponsibleRepository studentResponsibleRepository;

    public StudentResponsibleResponse findById(Integer id) {
        return StudentResponsibleMapper.toResponse(Objects.requireNonNull(studentResponsibleRepository.findById(id).orElse(null)));
    }
}