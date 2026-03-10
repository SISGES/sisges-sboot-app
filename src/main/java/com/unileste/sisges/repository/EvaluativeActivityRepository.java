package com.unileste.sisges.repository;

import com.unileste.sisges.model.EvaluativeActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluativeActivityRepository extends JpaRepository<EvaluativeActivity, Integer> {

    List<EvaluativeActivity> findByClassMeetingIdAndDeletedAtIsNullOrderByCreatedAtDesc(Integer classMeetingId);
}
