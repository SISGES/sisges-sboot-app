package com.unileste.sisges.repository;

import com.unileste.sisges.model.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Integer>, JpaSpecificationExecutor<SchoolClass> {

    Optional<SchoolClass> findByIdAndDeletedAtIsNull(Integer id);

    Optional<SchoolClass> findByNameAndDeletedAtIsNull(String name);

    Optional<SchoolClass> findByNameAndAcademicYearAndDeletedAtIsNull(String name, String academicYear);

    List<SchoolClass> findByAcademicYearAndDeletedAtIsNullOrderByNameAsc(String academicYear);
}
