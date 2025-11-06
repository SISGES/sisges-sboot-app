package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.request.LoginUserDto;
import com.unileste.sisges.controller.dto.request.RegisterUserRequest;
import com.unileste.sisges.model.User;
import com.unileste.sisges.service.AuthService;
import com.unileste.sisges.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterUserRequest registerUserDto) {
        User registeredUser = authService.signup(registerUserDto);

        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginUserDto loginUserDto) {
        User authenticatedUser = authService.authenticate(loginUserDto);

        String jwtToken = jwtService.generateToken(authenticatedUser);

        LoginResponse loginResponse = LoginResponse
                .builder()
                .token(jwtToken)
                .expiresIn(jwtService.getExpirationTime())
                .name(authenticatedUser.getName())
                .email(authenticatedUser.getEmail())
                .register(authenticatedUser.getRegister())
                .role(authenticatedUser.getUserRole().name())
                .build();

        return ResponseEntity.ok(loginResponse);
    }
}