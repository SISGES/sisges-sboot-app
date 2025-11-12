package com.unileste.sisges.specification;

import com.unileste.sisges.controller.dto.request.SearchClassRequest;
import com.unileste.sisges.model.SchoolClass;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ClassSpecification {

    public static Specification<SchoolClass> filterByDto(SearchClassRequest dto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (dto != null) {
                if (dto.getFromDate() != null && dto.getToDate() != null) {
                    predicates.add(cb.between(root.get("createdAt"), dto.getFromDate(), dto.getToDate()));
                } else if (dto.getFromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dto.getFromDate()));
                } else if (dto.getToDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dto.getToDate()));
                }

                if (dto.getName() != null) {
                    predicates.add(cb.equal(root.get("name"), dto.getName()));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}