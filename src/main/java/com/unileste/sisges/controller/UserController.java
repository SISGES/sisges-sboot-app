package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.auth.UserResponse;
import com.unileste.sisges.controller.dto.user.UserSearchRequest;
import com.unileste.sisges.controller.dto.user.UserSearchResponse;
import com.unileste.sisges.controller.dto.user.UpdateProfileRequest;
import com.unileste.sisges.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.findMe(principal.getId()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateMe(principal.getId(), request));
    }

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(
            @RequestBody(required = false) UserSearchRequest request) {
        List<UserSearchResponse> users = userService.searchUsers(request);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> findById(@PathVariable Integer id) {
        UserResponse user = userService.findById(id);
        return ResponseEntity.ok(user);
    }
}
