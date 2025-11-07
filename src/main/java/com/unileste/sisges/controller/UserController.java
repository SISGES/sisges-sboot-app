package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.request.UpdateUserRequest;
import com.unileste.sisges.controller.dto.response.UserResponse;
import com.unileste.sisges.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @PutMapping("/update/{id}")
    public ResponseEntity<UserResponse> update(@RequestBody UpdateUserRequest request, @PathVariable Integer id) {
        UserResponse response = userService.update(request, id);
        if (response == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(response);
    }
}