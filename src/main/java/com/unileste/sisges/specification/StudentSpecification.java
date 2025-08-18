package com.unileste.sisges.specification;

import com.unileste.sisges.controller.dto.request.SearchStudentDto;
import com.unileste.sisges.model.Student;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class StudentSpecification {

    public static Specification<Student> filterByDto(SearchStudentDto dto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (dto != null) {
                if (dto.getCreatedAtStart() != null && dto.getCreatedAtEnd() != null) {
                    predicates.add(cb.between(root.get("createdAt"), dto.getCreatedAtStart(), dto.getCreatedAtEnd()));
                } else if (dto.getCreatedAtStart() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dto.getCreatedAtStart()));
                } else if (dto.getCreatedAtEnd() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dto.getCreatedAtEnd()));
                }

                if (dto.getRegister() != null) {
                    predicates.add(cb.equal(root.get("register"), dto.getRegister()));
                }

                if (dto.getName() != null) {
                    predicates.add(cb.equal(root.get("name"), dto.getName()));
                }

                if (dto.getResponsible1Name() != null) {
                    predicates.add(cb.equal(root.get("responsible1Name"), dto.getResponsible1Name()));
                }

                if (dto.getEmail() != null) {
                    predicates.add(cb.like(cb.lower(root.get("responsible1Email")), "%" + dto.getEmail().toLowerCase() + "%"));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}