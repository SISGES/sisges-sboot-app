package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.request.CreateClassRequestDto;
import com.unileste.sisges.controller.dto.request.SearchClassRequest;
import com.unileste.sisges.controller.dto.request.UpdateClassRequestDto;
import com.unileste.sisges.controller.dto.response.SchoolClassResponse;
import com.unileste.sisges.controller.dto.response.DetailedSchoolClassResponse;
import com.unileste.sisges.controller.dto.response.SchoolClassSimpleResponse;
import com.unileste.sisges.enums.UserRoleENUM;
import com.unileste.sisges.exception.InvalidPayloadException;
import com.unileste.sisges.exception.InvalidRoleException;
import com.unileste.sisges.service.AuthService;
import com.unileste.sisges.service.SchoolClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/class")
@RequiredArgsConstructor
public class SchoolClassController {

    private final SchoolClassService schoolClassService;
    private final AuthService authService;

    @GetMapping("find/{id}")
    public ResponseEntity<SchoolClassSimpleResponse> findById(@PathVariable Integer id) throws InvalidPayloadException {
        SchoolClassSimpleResponse response = schoolClassService.findById(id);
        if (response == null) {
            throw new InvalidPayloadException("Usuário ou professor inexistente");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{classId}/bind/teacher/{teacherId}")
    public ResponseEntity<SchoolClassResponse> bindTeacher(@PathVariable Integer classId, @PathVariable Integer teacherId) throws InvalidPayloadException {
        SchoolClassResponse response = schoolClassService.bindTeacher(classId, teacherId);
        if (response == null) {
            throw new InvalidPayloadException("Usuário ou professor inexistente");
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{classId}/unbind/teacher/{teacherId}")
    public ResponseEntity<SchoolClassResponse> unbindTeacher(@PathVariable Integer classId, @PathVariable Integer teacherId) throws InvalidPayloadException {
        SchoolClassResponse response = schoolClassService.unbindTeacher(classId, teacherId);
        if (response == null) {
            throw new InvalidPayloadException("Usuário ou professor inexistente");
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{classId}/bind/student/{studentId}")
    public ResponseEntity<SchoolClassResponse> bindStudent(@PathVariable Integer classId, @PathVariable Integer studentId) throws InvalidPayloadException {
        SchoolClassResponse response = schoolClassService.bindStudent(classId, studentId);
        if (response == null) {
            throw new InvalidPayloadException("Usuário ou estudante inexistente");
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{classId}/unbind/student/{studentId}")
    public ResponseEntity<SchoolClassResponse> unbindStudent(@PathVariable Integer classId, @PathVariable Integer studentId) throws InvalidPayloadException {
        SchoolClassResponse response = schoolClassService.unbindStudent(classId, studentId);
        if (response == null) {
            throw new InvalidPayloadException("Usuário ou estudante inexistente");
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<SchoolClassResponse> create(@RequestBody @Valid CreateClassRequestDto request) throws InvalidPayloadException {
        UserRoleENUM userRole = authService.verifyUserRole();
        if (userRole == UserRoleENUM.STUDENT || userRole == UserRoleENUM.TEACHER)
            throw new InvalidRoleException("Usuário sem permissão");

        SchoolClassResponse response = schoolClassService.create(request);
        if (response == null) {
            throw new InvalidPayloadException(String.format("Já existe uma turma com o mesmo nome (%s).", request.getName()));
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    public ResponseEntity<Page<SchoolClassResponse>> search(@RequestBody(required = false) SearchClassRequest search) {
        UserRoleENUM userRole = authService.verifyUserRole();
        if (userRole == UserRoleENUM.STUDENT || userRole == UserRoleENUM.TEACHER)
            throw new InvalidRoleException("Usuário sem permissão");

        return ResponseEntity.ok(schoolClassService.search(search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetailedSchoolClassResponse> superFindById(@PathVariable Integer id) {
        UserRoleENUM userRole = authService.verifyUserRole();
        if (userRole == UserRoleENUM.STUDENT || userRole == UserRoleENUM.TEACHER)
            throw new InvalidRoleException("Usuário sem permissão");
        DetailedSchoolClassResponse response = schoolClassService.superFindById(id);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<SchoolClassResponse> update(@RequestBody @Valid UpdateClassRequestDto request, @PathVariable Integer id) throws InvalidPayloadException {
        UserRoleENUM userRole = authService.verifyUserRole();
        if (userRole == UserRoleENUM.STUDENT || userRole == UserRoleENUM.TEACHER)
            throw new InvalidRoleException("Usuário sem permissão");

        SchoolClassResponse response = schoolClassService.update(request, id);
        if (response == null) {
            throw new InvalidPayloadException("Ocorreu um erro. Verifique se não está inserindo um nome já existente.");
        }

        return ResponseEntity.ok(response);
    }
}