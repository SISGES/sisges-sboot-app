package com.unileste.sisges.repository;

import com.unileste.sisges.model.StudentYearResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentYearResultRepository extends JpaRepository<StudentYearResult, Integer> {

    boolean existsByAcademicCycle_Id(Integer academicCycleId);
}

