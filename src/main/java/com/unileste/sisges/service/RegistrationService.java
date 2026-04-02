package com.unileste.sisges.service;

import com.unileste.sisges.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;

    private static final String STUDENT_PREFIX = "A";
    private static final int STUDENT_DIGITS = 5;

    private static final String TEACHER_PREFIX = "P";
    private static final int TEACHER_DIGITS = 4;

    private static final String ADMIN_PREFIX = "ADM";
    private static final int ADMIN_DIGITS = 4;

    public String generateRegister(String role) {
        return switch (role.toUpperCase()) {
            case "STUDENT" -> generateNextRegister(STUDENT_PREFIX, STUDENT_DIGITS);
            case "TEACHER" -> generateNextRegister(TEACHER_PREFIX, TEACHER_DIGITS);
            case "ADMIN" -> generateNextRegister(ADMIN_PREFIX, ADMIN_DIGITS);
            default -> throw new IllegalArgumentException("Papel inválido para geração de matrícula: " + role);
        };
    }

    public String generateEmail(String register) {
        return register.toLowerCase() + "@sisges.com";
    }

    private String generateNextRegister(String prefix, int digits) {
        return userRepository.findLastRegisterByPrefix(prefix)
                .map(lastRegister -> incrementRegister(lastRegister, prefix, digits))
                .orElse(formatRegister(prefix, 1, digits));
    }

    private String incrementRegister(String lastRegister, String prefix, int digits) {
        String numericPart = lastRegister.substring(prefix.length());
        int nextNumber = Integer.parseInt(numericPart) + 1;
        return formatRegister(prefix, nextNumber, digits);
    }

    private String formatRegister(String prefix, int number, int digits) {
        String format = "%s%0" + digits + "d";
        return String.format(format, prefix, number);
    }
}
