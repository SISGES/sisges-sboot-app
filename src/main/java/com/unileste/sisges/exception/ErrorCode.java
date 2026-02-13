package com.unileste.sisges.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Códigos de erro padronizados da API do SISGES.
 * Cada código possui um HTTP status associado e uma mensagem padrão.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos"),
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Não autorizado. Token ausente ou inválido"),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "Sem permissão para acessar este recurso"),

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Erro de validação"),

    BUSINESS_RULE_VIOLATION(HttpStatus.BAD_REQUEST, "Violação de regra de negócio"),

    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Recurso não encontrado"),

    DATA_CONFLICT(HttpStatus.CONFLICT, "Conflito de dados. Verifique se os dados não estão duplicados ou inconsistentes"),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
