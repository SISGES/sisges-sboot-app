package com.unileste.sisges.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resposta de erro padronizada da API do SISGES.
 * <p>
 * Campos obrigatórios: status, code, message, timestamp.
 * O campo "errors" é incluído apenas quando há erros de validação de campos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;

    private String code;

    private String message;

    private LocalDateTime timestamp;

    private List<FieldValidationError> errors;

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .status(errorCode.getHttpStatus().value())
                .code(errorCode.name())
                .message(errorCode.getDefaultMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return ErrorResponse.builder()
                .status(errorCode.getHttpStatus().value())
                .code(errorCode.name())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse ofValidation(List<FieldValidationError> fieldErrors) {
        return ErrorResponse.builder()
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus().value())
                .code(ErrorCode.VALIDATION_ERROR.name())
                .message(ErrorCode.VALIDATION_ERROR.getDefaultMessage())
                .timestamp(LocalDateTime.now())
                .errors(fieldErrors)
                .build();
    }
}
