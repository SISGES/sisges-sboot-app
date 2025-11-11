package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.request.CreateClassRequestDto;
import com.unileste.sisges.controller.dto.request.SearchClassDto;
import com.unileste.sisges.controller.dto.request.UpdateClassRequestDto;
import com.unileste.sisges.controller.dto.response.SchoolClassResponse;
import com.unileste.sisges.controller.dto.response.DetailedSchoolClassResponse;
import com.unileste.sisges.enums.UserRoleENUM;
import com.unileste.sisges.exception.InvalidPayloadException;
import com.unileste.sisges.service.AuthService;
import com.unileste.sisges.service.SchoolClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/class")
@RequiredArgsConstructor
public class SchoolClassController {

    private final SchoolClassService schoolClassService;
    private final AuthService authService;

    @PostMapping("/create")
    public ResponseEntity<SchoolClassResponse> create(@RequestBody @Valid CreateClassRequestDto request) throws InvalidPayloadException {
        UserRoleENUM userRole = authService.verifyUserRole();
        if (userRole != UserRoleENUM.ADMIN && userRole != UserRoleENUM.DEV_ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        SchoolClassResponse response = schoolClassService.create(request);
        if (response == null) {
            throw new InvalidPayloadException(String.format("Já existe uma turma com o mesmo nome (%s).", request.getName()));
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    public ResponseEntity<Page<SchoolClassResponse>> search(@RequestBody @Valid SearchClassDto search) {
        return ResponseEntity.ok(schoolClassService.search(search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetailedSchoolClassResponse> findById(@PathVariable Integer id) {
        DetailedSchoolClassResponse response = schoolClassService.findById(id);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<SchoolClassResponse> update(@RequestBody @Valid UpdateClassRequestDto request, @PathVariable Integer id) throws InvalidPayloadException {
        SchoolClassResponse response = schoolClassService.update(request, id);
        if (response == null) {
            throw new InvalidPayloadException("Ocorreu um erro. Verifique se não está inserindo um nome já existente.");
        }
        return ResponseEntity.ok(response);
    }
}