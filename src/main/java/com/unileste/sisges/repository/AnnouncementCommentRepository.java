package com.unileste.sisges.repository;

import com.unileste.sisges.model.AnnouncementComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementCommentRepository extends JpaRepository<AnnouncementComment, Integer> {

    long countByAnnouncementIdAndDeletedAtIsNull(Integer announcementId);

    List<AnnouncementComment> findByAnnouncementIdAndDeletedAtIsNullOrderByCreatedAtAsc(Integer announcementId);
}
