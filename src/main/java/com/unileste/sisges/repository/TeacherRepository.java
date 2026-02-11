package com.unileste.sisges.repository;

import com.unileste.sisges.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Integer>, JpaSpecificationExecutor<Teacher> {

    Optional<Teacher> findByIdAndDeletedAtIsNull(Integer id);
}
