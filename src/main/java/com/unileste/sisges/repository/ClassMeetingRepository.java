package com.unileste.sisges.repository;

import com.unileste.sisges.model.ClassMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ClassMeetingRepository extends JpaRepository<ClassMeeting, Integer>, JpaSpecificationExecutor<ClassMeeting> {

    Optional<ClassMeeting> findByIdAndDeletedAtIsNull(Integer id);

    @Query("""
            SELECT cm FROM ClassMeeting cm
            WHERE cm.schoolClass.id = :classId
            AND cm.meetingDate = :date
            AND cm.deletedAt IS NULL
            AND (cm.startTime < :endTime AND cm.endTime > :startTime)
            """)
    List<ClassMeeting> findOverlappingMeetings(
            @Param("classId") Integer classId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}
