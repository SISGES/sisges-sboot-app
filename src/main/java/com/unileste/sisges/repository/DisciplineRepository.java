package com.unileste.sisges.repository;

import com.unileste.sisges.model.Discipline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DisciplineRepository extends JpaRepository<Discipline, Integer> {

    List<Discipline> findAllByDeletedAtIsNull();

    Optional<Discipline> findByIdAndDeletedAtIsNull(Integer id);

    boolean existsByNameAndDeletedAtIsNull(String name);
}