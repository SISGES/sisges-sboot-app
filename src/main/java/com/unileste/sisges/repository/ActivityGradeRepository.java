package com.unileste.sisges.repository;

import com.unileste.sisges.model.ActivityGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityGradeRepository extends JpaRepository<ActivityGrade, Integer> {

    List<ActivityGrade> findByActivityIdOrderByStudent_IdAsc(Integer activityId);

    Optional<ActivityGrade> findByActivityIdAndStudentId(Integer activityId, Integer studentId);

    List<ActivityGrade> findByStudentIdAndActivity_ClassMeeting_SchoolClass_Id(Integer studentId, Integer classId);
}

