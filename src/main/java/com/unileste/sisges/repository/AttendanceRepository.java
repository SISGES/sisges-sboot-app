package com.unileste.sisges.repository;

import com.unileste.sisges.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {

    Optional<Attendance> findByClassMeeting_IdAndStudent_Id(Integer classMeetingId, Integer studentId);
}
