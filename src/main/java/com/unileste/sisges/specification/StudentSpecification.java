package com.unileste.sisges.specification;

import com.unileste.sisges.controller.dto.request.SearchStudentRequest;
import com.unileste.sisges.model.Student;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class StudentSpecification {

    public static Specification<Student> filter(SearchStudentRequest dto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (dto.getFromDate() != null && dto.getToDate() != null) {
                predicates.add(cb.between(root.get("createdAt"), dto.getFromDate(), dto.getToDate()));
            } else if (dto.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dto.getFromDate()));
            } else if (dto.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dto.getToDate()));
            }

            if (dto.getRegister() != null) {
                predicates.add(cb.equal(root.get("register"), dto.getRegister()));
            }

            if (dto.getName() != null) {
                predicates.add(cb.equal(root.get("name"), dto.getName()));
            }

            if (dto.getResponsibleName() != null) {
                predicates.add(cb.equal(root.get("responsible1Name"), dto.getResponsibleName()));
            }

            if (dto.getEmail() != null) {
                predicates.add(cb.like(cb.lower(root.get("responsible1Email")), "%" + dto.getEmail().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}