package com.unileste.sisges.repository;

import com.unileste.sisges.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {

    @Query("""
        SELECT a FROM Announcement a
        WHERE a.deletedAt IS NULL
        AND (a.activeFrom IS NULL OR a.activeFrom <= :now)
        AND (a.activeUntil IS NULL OR a.activeUntil >= :now)
        AND (
            (a.hiddenForRoles IS NULL OR TRIM(a.hiddenForRoles) = '')
            OR (a.hiddenForRoles NOT LIKE CONCAT('%', :role, '%'))
        )
        ORDER BY a.createdAt DESC
        """)
    List<Announcement> findActiveForRole(@Param("role") String role, @Param("now") LocalDateTime now);

    List<Announcement> findAllByDeletedAtIsNullOrderByCreatedAtDesc();
}
