package com.unileste.sisges.repository;

import com.unileste.sisges.model.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClassRepository extends JpaRepository<ClassEntity, Integer>, JpaSpecificationExecutor<ClassEntity> {
    boolean existsByName(String name);
}