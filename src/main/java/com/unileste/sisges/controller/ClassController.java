package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.request.CreateClassRequestDto;
import com.unileste.sisges.controller.dto.response.ClassResponseDto;
import com.unileste.sisges.exception.InvalidPayloadException;
import com.unileste.sisges.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/class")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @PostMapping("/create")
    public ResponseEntity<ClassResponseDto> create(@RequestBody @Valid CreateClassRequestDto request) throws InvalidPayloadException {
        ClassResponseDto response = classService.create(request);
        if (response == null) {
            throw new InvalidPayloadException(String.format("Já existe uma turma com o mesmo nome (%s).", request.getName()));
        }
        return ResponseEntity.ok(response);
    }
}