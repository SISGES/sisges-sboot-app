package com.unileste.sisges.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = AllowedAnnouncementTtlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedAnnouncementTtl {

    String message() default "TTL deve ser 1, 4, 10, 24, 48 ou 168 horas";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
