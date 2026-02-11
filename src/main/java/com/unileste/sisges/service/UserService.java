package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.auth.UserResponse;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserResponse> listAllUsers() {
        return userRepository.findAllByDeletedAtIsNull()
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .register(user.getRegister())
                .role(user.getUserRole())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .build();
    }
}
