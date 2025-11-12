package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.request.SearchUserRequest;
import com.unileste.sisges.controller.dto.request.UpdateUserRequest;
import com.unileste.sisges.controller.dto.response.UserResponse;
import com.unileste.sisges.enums.UserRoleENUM;
import com.unileste.sisges.service.AuthService;
import com.unileste.sisges.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<Page<UserResponse>> search(@RequestBody(required = false) SearchUserRequest search) {
        UserRoleENUM userRole = authService.verifyUserRole();
        if (userRole == UserRoleENUM.STUDENT)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return ResponseEntity.ok(userService.search(search, userRole));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<UserResponse> update(@RequestBody UpdateUserRequest request, @PathVariable Integer id) {
        UserResponse response = userService.update(request, id);
        if (response == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(response);
    }
}