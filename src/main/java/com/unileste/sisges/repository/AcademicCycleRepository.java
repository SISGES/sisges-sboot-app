package com.unileste.sisges.repository;

import com.unileste.sisges.model.AcademicCycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcademicCycleRepository extends JpaRepository<AcademicCycle, Integer> {

    Optional<AcademicCycle> findFirstByOrderByIdAsc();
}

