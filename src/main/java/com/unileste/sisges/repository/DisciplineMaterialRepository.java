package com.unileste.sisges.repository;

import com.unileste.sisges.model.DisciplineMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisciplineMaterialRepository extends JpaRepository<DisciplineMaterial, Integer> {

    List<DisciplineMaterial> findByDisciplineIdAndDeletedAtIsNullOrderByCreatedAtDesc(Integer disciplineId);

    List<DisciplineMaterial> findBySchoolClassIdAndDeletedAtIsNullOrderByCreatedAtDesc(Integer classId);

    List<DisciplineMaterial> findByDisciplineIdAndSchoolClassIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            Integer disciplineId, Integer classId);
}
