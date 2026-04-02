package com.unileste.sisges.repository.specification;

import com.unileste.sisges.controller.dto.classmeeting.ClassMeetingSearchRequest;
import com.unileste.sisges.model.ClassMeeting;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ClassMeetingSpecification {

    private ClassMeetingSpecification() {
    }

    public static Specification<ClassMeeting> withFilters(ClassMeetingSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isNull(root.get("deletedAt")));

            if (request != null) {
                if (request.getDate() != null) {
                    predicates.add(cb.equal(root.get("meetingDate"), request.getDate()));
                }
                if (request.getDisciplineId() != null) {
                    predicates.add(cb.equal(root.get("discipline").get("id"), request.getDisciplineId()));
                }
                if (request.getClassId() != null) {
                    predicates.add(cb.equal(root.get("schoolClass").get("id"), request.getClassId()));
                }
                if (request.getTeacherId() != null) {
                    predicates.add(cb.equal(root.get("teacher").get("id"), request.getTeacherId()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<ClassMeeting> withFiltersAndTeacher(
            ClassMeetingSearchRequest request,
            Integer teacherId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(cb.equal(root.get("teacher").get("id"), teacherId));
            if (request != null) {
                if (request.getDate() != null) {
                    predicates.add(cb.equal(root.get("meetingDate"), request.getDate()));
                }
                if (request.getDisciplineId() != null) {
                    predicates.add(cb.equal(root.get("discipline").get("id"), request.getDisciplineId()));
                }
                if (request.getClassId() != null) {
                    predicates.add(cb.equal(root.get("schoolClass").get("id"), request.getClassId()));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
