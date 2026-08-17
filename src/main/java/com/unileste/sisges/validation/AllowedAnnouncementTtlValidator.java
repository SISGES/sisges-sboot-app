package com.unileste.sisges.validation;

import com.unileste.sisges.service.AnnouncementTtlHours;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AllowedAnnouncementTtlValidator implements ConstraintValidator<AllowedAnnouncementTtl, Integer> {

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return AnnouncementTtlHours.isAllowed(value);
    }
}
