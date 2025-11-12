package com.unileste.sisges.specification;

import com.unileste.sisges.controller.dto.request.SearchUserRequest;
import com.unileste.sisges.model.User;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class UserSpecification {

    public static Specification<User> filter(SearchUserRequest dto) {
        return ((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (dto.getInitialBirthDate() != null && dto.getFinalBirthDate() != null) {
                predicates.add(cb.between(root.get("birthDate"), dto.getInitialBirthDate(), dto.getFinalBirthDate()));
            } else if (dto.getInitialBirthDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("birthDate"), dto.getInitialBirthDate()));
            } else if (dto.getFinalBirthDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("birthDate"), dto.getFinalBirthDate()));
            }

            if (dto.getName() != null) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + dto.getName() + "%"));
            }

            if (dto.getEmail() != null) {
                predicates.add(cb.like(root.get("email"), "%" + dto.getEmail() + "%"));
            }

            if (dto.getRegister() != null) {
                predicates.add(cb.like(root.get("register"), "%" + dto.getRegister() + "%"));
            }

            if (dto.getGender() != null) {
                predicates.add(cb.equal(root.get("gender"), dto.getGender()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }
}