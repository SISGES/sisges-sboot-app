package com.unileste.sisges.service;

import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

//    public User save(StudentRequest request, String register) {
//        return userRepository.save(User
//                .builder()
//                .name(request.getBaseData().getName())
//                .email(request.getBaseData().getEmail())
//                .password(request.getBaseData().getPassword())
//                .birthDate(request.getBaseData().getBirthDate())
//                .register(register)
//                .userRole(UserRoleENUM.fromCode(request.getBaseData().getUserRole()))
//                .createdAt(LocalDateTime.now())
//                .build());
//    }

    public String getLastRegister(){
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "Register"));
        if (users.isEmpty()) {
            return null;
        }
        return users.getFirst().getRegister();
    }

    public User findById(Integer userId) {
        return userRepository.findById(userId).orElse(null);
    }
}