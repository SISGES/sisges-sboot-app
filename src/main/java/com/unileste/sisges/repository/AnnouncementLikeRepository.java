package com.unileste.sisges.repository;

import com.unileste.sisges.model.AnnouncementLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnnouncementLikeRepository extends JpaRepository<AnnouncementLike, Integer> {

    long countByAnnouncementId(Integer announcementId);

    List<AnnouncementLike> findByAnnouncementIdOrderByCreatedAtAsc(Integer announcementId);

    Optional<AnnouncementLike> findByAnnouncementIdAndUserId(Integer announcementId, Integer userId);

    boolean existsByAnnouncementIdAndUserId(Integer announcementId, Integer userId);
}
