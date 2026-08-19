package com.unileste.sisges.repository;

import com.unileste.sisges.model.SchoolEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface SchoolEventRepository extends JpaRepository<SchoolEvent, Integer> {
    List<SchoolEvent> findByEventAtGreaterThanEqualOrderByEventAtAsc(LocalDateTime now);
}
