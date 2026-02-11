package com.unileste.sisges.repository.specification;

import com.unileste.sisges.controller.dto.schoolclass.SchoolClassSearchRequest;
import com.unileste.sisges.model.SchoolClass;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class SchoolClassSpecification {

    private SchoolClassSpecification() {
    }

    public static Specification<SchoolClass> withFilters(SchoolClassSearchRequest request) {
        return (Root<SchoolClass> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isNull(root.get("deletedAt")));

            if (request != null) {
                if (request.getName() != null && !request.getName().isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("name")),
                            "%" + request.getName().toLowerCase() + "%"));
                }

                if (request.getAcademicYear() != null && !request.getAcademicYear().isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("academicYear")),
                            "%" + request.getAcademicYear().toLowerCase() + "%"));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}