package com.unileste.sisges.repository;

import com.unileste.sisges.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Integer> {
}
