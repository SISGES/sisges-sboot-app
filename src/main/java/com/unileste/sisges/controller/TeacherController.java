package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.request.SearchTeacherRequest;
import com.unileste.sisges.controller.dto.response.TeacherResponse;
import com.unileste.sisges.enums.UserRoleENUM;
import com.unileste.sisges.exception.InvalidRoleException;
import com.unileste.sisges.service.AuthService;
import com.unileste.sisges.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;
    private final AuthService authService;

    @PostMapping("/search")
    public ResponseEntity<Page<TeacherResponse>> search(@RequestBody(required = false) SearchTeacherRequest search) {
        UserRoleENUM userRole = authService.verifyUserRole();
        if (userRole == UserRoleENUM.STUDENT || userRole == UserRoleENUM.TEACHER)
            throw new InvalidRoleException("Usuário sem permissão");

        return ResponseEntity.ok(teacherService.search(search));
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<TeacherResponse> findByUserId(@PathVariable Integer id) {
        TeacherResponse response = teacherService.findByUserId(id);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }
}