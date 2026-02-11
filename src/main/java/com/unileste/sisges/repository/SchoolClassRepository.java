package com.unileste.sisges.repository;

import com.unileste.sisges.model.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Integer>, JpaSpecificationExecutor<SchoolClass> {

    Optional<SchoolClass> findByIdAndDeletedAtIsNull(Integer id);
}