package com.unileste.sisges.repository;

import com.unileste.sisges.model.EvaluativeActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluativeActivityRepository extends JpaRepository<EvaluativeActivity, Integer> {

    List<EvaluativeActivity> findByClassMeetingIdAndDeletedAtIsNullOrderByCreatedAtDesc(Integer classMeetingId);

    List<EvaluativeActivity> findByClassMeeting_SchoolClass_IdAndDeletedAtIsNullOrderByCreatedAtDesc(Integer classId);

    List<EvaluativeActivity> findByDeletedAtIsNullAndReleasedFalse();

    List<EvaluativeActivity> findByDeletedAtIsNullAndReleasedFalseAndTrimesterNumber(Integer trimesterNumber);
}
