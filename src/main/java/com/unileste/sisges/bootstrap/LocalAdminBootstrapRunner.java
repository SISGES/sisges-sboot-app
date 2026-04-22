package com.unileste.sisges.bootstrap;

import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Cria o usuário administrador local (email/senha fixos) quando habilitado.
 * Idempotente: não altera a conta se o email já existir.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
@ConditionalOnProperty(name = "sisges.local-admin.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class LocalAdminBootstrapRunner implements ApplicationRunner {

    private static final String EMAIL = "admin.local@sisges.com";
    private static final String PASSWORD = "admin123";
    private static final String REGISTER = "ADMLOCL1";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(EMAIL)) {
            return;
        }
        if (userRepository.existsByRegisterAndDeletedAtIsNull(REGISTER)) {
            log.warn("Registro {} já em uso; não foi criado o admin local.", REGISTER);
            return;
        }
        User user = User.builder()
                .name("Administrador Local")
                .email(EMAIL)
                .register(REGISTER)
                .password(passwordEncoder.encode(PASSWORD))
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender("MALE")
                .userRole("ADMIN")
                .build();
        userRepository.save(user);
        log.info("Usuário admin local criado: {} (registro {})", EMAIL, REGISTER);
    }
}
