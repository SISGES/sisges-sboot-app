package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.auth.UserResponse;
import com.unileste.sisges.controller.dto.user.UserSearchRequest;
import com.unileste.sisges.controller.dto.user.UserSearchResponse;
import com.unileste.sisges.controller.dto.user.UpdateProfileRequest;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.UserRepository;
import com.unileste.sisges.repository.specification.UserSpecification;
import com.unileste.sisges.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;

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

    public UserResponse findMe(Integer id) {
        return findById(id);
    }

    @Transactional
    public UserResponse updateMe(Integer id, UpdateProfileRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
        if (!List.of("ADMIN", "TEACHER").contains(user.getUserRole().toUpperCase())) {
            throw new org.springframework.security.access.AccessDeniedException("Perfil não editável");
        }
        if (request.getName() != null && !request.getName().isBlank()) user.setName(request.getName().trim());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getProfileImagePath() != null && !request.getProfileImagePath().equals(user.getProfileImagePath())) {
            if (user.getProfileImagePath() != null) storageService.delete(user.getProfileImagePath());
            user.setProfileImagePath(request.getProfileImagePath().isBlank() ? null : request.getProfileImagePath());
        }
        return toUserResponse(userRepository.save(user));
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
                .profileImagePath(user.getProfileImagePath())
                .build();
    }
}
