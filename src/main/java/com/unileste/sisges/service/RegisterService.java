package com.unileste.sisges.service;

import com.unileste.sisges.constants.Constants;
import com.unileste.sisges.enums.UserRoleENUM;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final UserService userService;

    public String generateRegister(UserRoleENUM userType) {
        String lastRegister = userService.getLastRegister();
        if (userType == UserRoleENUM.STUDENT) {
            if (lastRegister == null) return Constants.STUDENT_REGISTER_PREFIX + "1000";
            int lastRegisterNumbers = Integer.parseInt(lastRegister.substring(1)) + 1;
            return Constants.STUDENT_REGISTER_PREFIX + lastRegisterNumbers;
        }

        if (lastRegister == null) return Constants.STUDENT_REGISTER_PREFIX + "1000";
        int lastRegisterNumbers = Integer.parseInt(lastRegister.substring(1)) + 1;
        return Constants.TEACHER_REGISTER_PREFIX + lastRegisterNumbers;
    }
}