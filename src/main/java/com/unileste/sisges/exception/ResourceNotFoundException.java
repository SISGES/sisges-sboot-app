package com.unileste.sisges.exception;

/**
 * Exceção para recurso não encontrado (HTTP 404).
 * Lançar quando uma entidade buscada por ID ou critério não existir.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(resourceName + " não encontrado(a): " + identifier);
    }
}
