package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.request.UpdateUserRequest;
import com.unileste.sisges.controller.dto.response.UserResponse;
import com.unileste.sisges.mapper.UserMapper;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public String getLastRegister() {
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "Register"));
        if (users.isEmpty()) {
            return null;
        }
        return users.getFirst().getRegister();
    }

    public User findById(Integer userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public UserResponse update(UpdateUserRequest request, Integer id) {
        Optional<User> optUser = userRepository.findById(id);
        if (optUser.isEmpty()) return null;
        User user = optUser.get();
        if (request.getName() != null) user.setName(request.getName());
        if (request.getPassword() != null) {
            String encodedPassword = passwordEncoder.encode(request.getPassword());
            user.setPassword(encodedPassword);
        }
        if (request.getBirthDate() != null) user.setBirthDate(request.getBirthDate());
        if (request.getGender() != null) user.setGender(request.getGender());

        return UserMapper.toResponse(userRepository.save(user));
    }
}