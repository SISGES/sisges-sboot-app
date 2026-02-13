package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.auth.UserResponse;
import com.unileste.sisges.controller.dto.user.UserSearchRequest;
import com.unileste.sisges.controller.dto.user.UserSearchResponse;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.UserRepository;
import com.unileste.sisges.repository.specification.UserSpecification;
import com.unileste.sisges.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserSearchResponse> searchUsers(UserSearchRequest request) {
        Specification<User> spec = UserSpecification.withFilters(request);
        return userRepository.findAll(spec)
                .stream()
                .map(this::toUserSearchResponse)
                .toList();
    }

    public UserResponse findById(Integer id) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
        return toUserResponse(user);
    }

    private UserSearchResponse toUserSearchResponse(User user) {
        return UserSearchResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getUserRole())
                .build();
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
