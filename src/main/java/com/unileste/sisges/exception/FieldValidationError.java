package com.unileste.sisges.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa um erro de validação em um campo específico.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldValidationError {

    private String field;
    private String message;
}
