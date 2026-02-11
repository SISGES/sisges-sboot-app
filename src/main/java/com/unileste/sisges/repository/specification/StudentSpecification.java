package com.unileste.sisges.repository.specification;

import com.unileste.sisges.controller.dto.student.StudentSearchRequest;
import com.unileste.sisges.model.Student;
import com.unileste.sisges.model.User;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class StudentSpecification {

    private StudentSpecification() {
    }

    public static Specification<Student> withFilters(StudentSearchRequest request) {
        return (Root<Student> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isNull(root.get("deletedAt")));

            Join<Student, User> baseDataJoin = root.join("baseData", JoinType.INNER);
            predicates.add(cb.isNull(baseDataJoin.get("deletedAt")));

            if (request != null) {
                if (request.getName() != null && !request.getName().isBlank()) {
                    predicates.add(cb.like(cb.lower(baseDataJoin.get("name")),
                            "%" + request.getName().toLowerCase() + "%"));
                }

                if (request.getEmail() != null && !request.getEmail().isBlank()) {
                    predicates.add(cb.like(cb.lower(baseDataJoin.get("email")),
                            "%" + request.getEmail().toLowerCase() + "%"));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
