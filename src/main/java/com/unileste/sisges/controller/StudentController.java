package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.request.SearchStudentRequest;
import com.unileste.sisges.controller.dto.response.StudentResponse;
import com.unileste.sisges.enums.UserRoleENUM;
import com.unileste.sisges.exception.InvalidRoleException;
import com.unileste.sisges.model.Student;
import com.unileste.sisges.service.AuthService;
import com.unileste.sisges.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final AuthService authService;

    @PostMapping("/search")
    public ResponseEntity<Page<StudentResponse>> search(@RequestBody(required = false) SearchStudentRequest request) {
        UserRoleENUM userRole = authService.verifyUserRole();
        if (userRole == UserRoleENUM.STUDENT || userRole == UserRoleENUM.TEACHER)
            throw new InvalidRoleException("Usuário sem permissão");

        return ResponseEntity.ok(studentService.search(request));
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<StudentResponse> findByUserId(@PathVariable Integer id) {
        StudentResponse response = studentService.findByUserId(id);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> findById(@PathVariable Integer id) {
        StudentResponse response = studentService.findById(id);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<StudentResponse> delete(@PathVariable Integer id) {
        Student response = studentService.delete(id);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok().build();
    }
}