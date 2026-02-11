package com.unileste.sisges.repository.specification;

import com.unileste.sisges.controller.dto.user.UserSearchRequest;
import com.unileste.sisges.model.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<User> withFilters(UserSearchRequest request) {
        return (Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Sempre filtrar registros não deletados (soft delete)
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (request != null) {
                if (request.getName() != null && !request.getName().isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("name")),
                            "%" + request.getName().toLowerCase() + "%"));
                }

                if (request.getEmail() != null && !request.getEmail().isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("email")),
                            "%" + request.getEmail().toLowerCase() + "%"));
                }

                if (request.getRegister() != null && !request.getRegister().isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("register")),
                            "%" + request.getRegister().toLowerCase() + "%"));
                }

                if (request.getGender() != null && !request.getGender().isBlank()) {
                    predicates.add(cb.equal(cb.lower(root.get("gender")),
                            request.getGender().toLowerCase()));
                }

                if (request.getInitialDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("birthDate"),
                            request.getInitialDate()));
                }

                if (request.getFinalDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("birthDate"),
                            request.getFinalDate()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
