package com.unileste.sisges.service;

import com.unileste.sisges.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável pela geração automática de matrículas e e-mails dos usuários.
 * 
 * Regras de matrícula:
 * - Alunos: A00001, A00002, ... (prefixo "A" + 5 dígitos)
 * - Professores: P0001, P0002, ... (prefixo "P" + 4 dígitos)
 * - Administradores: ADM0001, ADM0002, ... (prefixo "ADM" + 4 dígitos)
 * 
 * E-mail é gerado automaticamente: matricula@sisges.com
 */
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

    /**
     * Gera a próxima matrícula disponível baseado no papel do usuário.
     *
     * @param role Papel do usuário (STUDENT, TEACHER ou ADMIN)
     * @return Próxima matrícula disponível
     */
    public String generateRegister(String role) {
        return switch (role.toUpperCase()) {
            case "STUDENT" -> generateNextRegister(STUDENT_PREFIX, STUDENT_DIGITS);
            case "TEACHER" -> generateNextRegister(TEACHER_PREFIX, TEACHER_DIGITS);
            case "ADMIN" -> generateNextRegister(ADMIN_PREFIX, ADMIN_DIGITS);
            default -> throw new IllegalArgumentException("Papel inválido para geração de matrícula: " + role);
        };
    }

    /**
     * Gera o e-mail baseado na matrícula.
     * Formato: matricula@sisges.com
     *
     * @param register Matrícula do usuário
     * @return E-mail gerado
     */
    public String generateEmail(String register) {
        return register.toLowerCase() + "@sisges.com";
    }

    /**
     * Gera a próxima matrícula disponível para um determinado prefixo.
     *
     * @param prefix Prefixo da matrícula (A, P, ADM)
     * @param digits Quantidade de dígitos numéricos
     * @return Próxima matrícula disponível
     */
    private String generateNextRegister(String prefix, int digits) {
        return userRepository.findLastRegisterByPrefix(prefix)
                .map(lastRegister -> incrementRegister(lastRegister, prefix, digits))
                .orElse(formatRegister(prefix, 1, digits));
    }

    /**
     * Incrementa uma matrícula existente.
     *
     * @param lastRegister Última matrícula encontrada
     * @param prefix Prefixo da matrícula
     * @param digits Quantidade de dígitos
     * @return Nova matrícula incrementada
     */
    private String incrementRegister(String lastRegister, String prefix, int digits) {
        String numericPart = lastRegister.substring(prefix.length());
        int nextNumber = Integer.parseInt(numericPart) + 1;
        return formatRegister(prefix, nextNumber, digits);
    }

    /**
     * Formata a matrícula com zeros à esquerda.
     *
     * @param prefix Prefixo da matrícula
     * @param number Número sequencial
     * @param digits Quantidade de dígitos
     * @return Matrícula formatada
     */
    private String formatRegister(String prefix, int number, int digits) {
        String format = "%s%0" + digits + "d";
        return String.format(format, prefix, number);
    }
}
