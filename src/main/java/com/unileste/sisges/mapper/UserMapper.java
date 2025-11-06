package com.unileste.sisges.mapper;

import com.unileste.sisges.controller.dto.request.RegisterUserRequest;
import com.unileste.sisges.controller.dto.response.UserResponse;
import com.unileste.sisges.enums.GenderENUM;
import com.unileste.sisges.enums.UserRoleENUM;
import com.unileste.sisges.model.User;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

@UtilityClass
public class UserMapper {
    public static User toEntity(RegisterUserRequest request) {
        return User
                .builder()
                .name(request.getName())
                .email(request.getEmail())
                .birthDate(request.getBirthDate())
                .gender(GenderENUM.fromCode(request.getGender()).getCode())
                .userRole(UserRoleENUM.fromCode(request.getUserRole()))
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static UserResponse toResponse(User entity) {
        return UserResponse
                .builder()
                .name(entity.getName())
                .email(entity.getEmail())
                .gender(GenderENUM.fromCode(entity.getGender()))
                .birthDate(entity.getBirthDate())
                .register(entity.getRegister())
                .build();
    }
}