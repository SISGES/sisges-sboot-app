package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.auth.UserResponse;
import com.unileste.sisges.controller.dto.user.UserSearchRequest;
import com.unileste.sisges.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @RequestBody(required = false) UserSearchRequest request) {
        List<UserResponse> users = userService.searchUsers(request);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> findById(@PathVariable Integer id) {
        UserResponse user = userService.findById(id);
        return ResponseEntity.ok(user);
    }
}
