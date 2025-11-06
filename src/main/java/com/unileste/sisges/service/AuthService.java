package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.request.LoginUserDto;
import com.unileste.sisges.controller.dto.request.RegisterUserRequest;
import com.unileste.sisges.controller.dto.request.StudentRequest;
import com.unileste.sisges.enums.UserRoleENUM;
import com.unileste.sisges.mapper.UserMapper;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RegisterService registerService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public User signup(RegisterUserRequest input) {
        User user = UserMapper.toEntity(input);
        String encodedPassword = passwordEncoder.encode(input.getPassword());
        user.setPassword(encodedPassword);

        user.setRegister(registerService.generateRegister(UserRoleENUM.fromCode(input.getUserRole())));

        User createdUser = userRepository.save(user);
        this.createCorrectEntity(createdUser);

        return createdUser;
    }

    public User authenticate(LoginUserDto input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getEmail(),
                        input.getPassword()
                )
        );

        return userRepository.findByEmail(input.getEmail())
                .orElseThrow();
    }

    private void createCorrectEntity(User entity) {
        if (entity.getUserRole() == UserRoleENUM.STUDENT) {
            studentService.create(StudentRequest
                    .builder()
                    .userId(entity.getId())
                    .register(entity.getRegister())
                    .build());
        } else {
//            teacherService.create(TeacherRequest);
        }
    }
}