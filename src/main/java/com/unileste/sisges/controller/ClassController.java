package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.request.CreateClassRequestDto;
import com.unileste.sisges.controller.dto.request.SearchClassDto;
import com.unileste.sisges.controller.dto.request.UpdateClassRequestDto;
import com.unileste.sisges.controller.dto.response.ClassResponse;
import com.unileste.sisges.controller.dto.response.DetailedClassResponse;
import com.unileste.sisges.exception.InvalidPayloadException;
import com.unileste.sisges.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/class")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @PostMapping("/search")
    public ResponseEntity<Page<ClassResponse>> search(@RequestBody @Valid SearchClassDto search) {
        return ResponseEntity.ok(classService.search(search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetailedClassResponse> findById(@PathVariable Integer id) {
        DetailedClassResponse response = classService.findById(id);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<ClassResponse> create(@RequestBody @Valid CreateClassRequestDto request) throws InvalidPayloadException {
        ClassResponse response = classService.create(request);
        if (response == null) {
            throw new InvalidPayloadException(String.format("Já existe uma turma com o mesmo nome (%s).", request.getName()));
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ClassResponse> update(@RequestBody @Valid UpdateClassRequestDto request, @PathVariable Integer id) throws InvalidPayloadException {
        ClassResponse response = classService.update(request, id);
        if (response == null) {
            throw new InvalidPayloadException("Ocorreu um erro. Verifique se não está inserindo um nome já existente.");
        }
        return ResponseEntity.ok(response);
    }
}