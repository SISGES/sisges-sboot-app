package com.unileste.sisges.exception;

import org.apache.coyote.BadRequestException;

public class InvalidPayloadException extends BadRequestException {
    public InvalidPayloadException(String message) {
        super(message);
    }
}