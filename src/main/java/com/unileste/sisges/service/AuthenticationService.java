package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.request.LoginUserDto;
import com.unileste.sisges.controller.dto.request.RegisterUserDto;
import com.unileste.sisges.model.BaseUser;
import com.unileste.sisges.repository.BaseUserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthenticationService {

    private final BaseUserRepository baseUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(
            BaseUserRepository baseUserRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.baseUserRepository = baseUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public BaseUser signup(RegisterUserDto input) {
        BaseUser user = BaseUser
                .builder()
                .name(input.getFullName())
                .email(input.getEmail())
                .password(passwordEncoder.encode(input.getPassword()))
                .createdAt(LocalDateTime.now())
                .build();

        return baseUserRepository.save(user);
    }

    public BaseUser authenticate(LoginUserDto input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getEmail(),
                        input.getPassword()
                )
        );

        return baseUserRepository.findByEmail(input.getEmail())
                .orElseThrow();
    }
}