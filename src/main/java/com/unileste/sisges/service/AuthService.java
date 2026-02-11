package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.auth.LoginRequest;
import com.unileste.sisges.controller.dto.auth.LoginResponse;
import com.unileste.sisges.controller.dto.auth.RegisterUserRequest;
import com.unileste.sisges.controller.dto.auth.UserResponse;
import com.unileste.sisges.exception.BusinessRuleException;
import com.unileste.sisges.exception.ResourceNotFoundException;
import com.unileste.sisges.model.SchoolClass;
import com.unileste.sisges.model.Student;
import com.unileste.sisges.model.StudentResponsible;
import com.unileste.sisges.model.Teacher;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.SchoolClassRepository;
import com.unileste.sisges.repository.StudentRepository;
import com.unileste.sisges.repository.StudentResponsibleRepository;
import com.unileste.sisges.repository.TeacherRepository;
import com.unileste.sisges.repository.UserRepository;
import com.unileste.sisges.security.JwtService;
import com.unileste.sisges.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final StudentResponsibleRepository studentResponsibleRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(LoginResponse.UserInfo.builder()
                        .id(principal.getId())
                        .name(principal.getName())
                        .email(principal.getEmail())
                        .register(principal.getRegister())
                        .role(principal.getRole())
                        .build())
                .build();
    }

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new BusinessRuleException("E-mail já cadastrado: " + request.getEmail());
        }
        if (userRepository.existsByRegisterAndDeletedAtIsNull(request.getRegister())) {
            throw new BusinessRuleException("Registro/matrícula já cadastrado: " + request.getRegister());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .register(request.getRegister())
                .password(passwordEncoder.encode(request.getPassword()))
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .userRole(request.getRole())
                .build();

        user = userRepository.save(user);

        switch (request.getRole()) {
            case "TEACHER" -> {
                Teacher teacher = Teacher.builder()
                        .baseData(user)
                        .build();
                teacherRepository.save(teacher);
            }
            case "STUDENT" -> {
                if (request.getResponsibleId() == null && request.getResponsibleData() == null) {
                    throw new BusinessRuleException("Aluno deve ter um responsável legal (informe responsibleId ou responsibleData).");
                }
                StudentResponsible responsible = resolveResponsible(request);
                SchoolClass schoolClass = resolveSchoolClass(request);
                Student student = Student.builder()
                        .baseData(user)
                        .responsible(responsible)
                        .currentClass(schoolClass)
                        .build();
                studentRepository.save(student);
            }
            case "ADMIN" -> {
            }
            default -> throw new BusinessRuleException("Papel inválido: " + request.getRole());
        }

        return toUserResponse(user);
    }

    private StudentResponsible resolveResponsible(RegisterUserRequest request) {
        if (request.getResponsibleData() != null) {
            RegisterUserRequest.ResponsibleData rd = request.getResponsibleData();
            StudentResponsible sr = StudentResponsible.builder()
                    .name(rd.getName())
                    .phone(rd.getPhone())
                    .alternativePhone(rd.getAlternativePhone())
                    .email(rd.getEmail())
                    .alternativeEmail(rd.getAlternativeEmail())
                    .build();
            return studentResponsibleRepository.save(sr);
        }
        if (request.getResponsibleId() != null) {
            return studentResponsibleRepository.findById(request.getResponsibleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Responsável", request.getResponsibleId()));
        }
        return null;
    }

    private SchoolClass resolveSchoolClass(RegisterUserRequest request) {
        if (request.getClassId() != null) {
            return schoolClassRepository.findById(request.getClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Turma", request.getClassId()));
        }
        return null;
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
