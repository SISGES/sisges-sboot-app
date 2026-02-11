package com.unileste.sisges.repository;

import com.unileste.sisges.model.Discipline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DisciplineRepository extends JpaRepository<Discipline, Integer> {

    Optional<Discipline> findByIdAndDeletedAtIsNull(Integer id);
}