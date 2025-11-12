package com.unileste.sisges.specification;

import com.unileste.sisges.controller.dto.request.SearchTeacherRequest;
import com.unileste.sisges.model.Teacher;
import com.unileste.sisges.model.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class TeacherSpecification {

    public static Specification<Teacher> filter(SearchTeacherRequest dto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Teacher, User> userJoin = root.join("baseData");
            predicates.add(cb.isNull(userJoin.get("deletedAt")));

            if (dto != null) {
                if (dto.getFromDate() != null && dto.getToDate() != null) {
                    predicates.add(cb.between(userJoin.get("createdAt"), dto.getFromDate(), dto.getToDate()));
                } else if (dto.getFromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(userJoin.get("createdAt"), dto.getFromDate()));
                } else if (dto.getToDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(userJoin.get("createdAt"), dto.getToDate()));
                }

                if (dto.getName() != null && !dto.getName().isBlank()) {
                    predicates.add(cb.like(cb.lower(userJoin.get("name")), "%" + dto.getName().toLowerCase() + "%"));
                }

                if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
                    predicates.add(cb.like(cb.lower(userJoin.get("email")), "%" + dto.getEmail().toLowerCase() + "%"));
                }

                if (dto.getRegister() != null && !dto.getRegister().isBlank()) {
                    predicates.add(cb.like(cb.lower(userJoin.get("register")), "%" + dto.getRegister().toLowerCase() + "%"));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
