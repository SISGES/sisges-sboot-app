package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.announcement.AnnouncementLikesResponse;
import com.unileste.sisges.controller.dto.announcement.UserSimpleResponse;
import com.unileste.sisges.model.Announcement;
import com.unileste.sisges.model.AnnouncementLike;
import com.unileste.sisges.repository.AnnouncementLikeRepository;
import com.unileste.sisges.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementLikeService {

    private final AnnouncementLikeRepository likeRepository;
    private final AnnouncementRepository announcementRepository;
    private final com.unileste.sisges.repository.UserRepository userRepository;

    @Transactional
    public boolean toggleLike(Integer announcementId, Integer userId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Anúncio não encontrado"));
        if (announcement.getDeletedAt() != null) {
            throw new RuntimeException("Anúncio não encontrado");
        }
        if (userId == null) {
            throw new RuntimeException("Usuário não autenticado");
        }

        var existing = likeRepository.findByAnnouncementIdAndUserId(announcementId, userId);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            return false; // unliked
        } else {
            var user = userRepository.getReferenceById(userId);
            AnnouncementLike like = AnnouncementLike.builder()
                    .announcement(announcement)
                    .user(user)
                    .build();
            likeRepository.save(like);
            return true; // liked
        }
    }

    @Transactional(readOnly = true)
    public AnnouncementLikesResponse getLikes(Integer announcementId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Anúncio não encontrado"));
        if (announcement.getDeletedAt() != null) {
            throw new RuntimeException("Anúncio não encontrado");
        }

        long count = likeRepository.countByAnnouncementId(announcementId);
        List<UserSimpleResponse> likedBy = likeRepository.findByAnnouncementIdOrderByCreatedAtAsc(announcementId)
                .stream()
                .map(al -> UserSimpleResponse.builder()
                        .id(al.getUser().getId())
                        .name(al.getUser().getName())
                        .build())
                .collect(Collectors.toList());

        return AnnouncementLikesResponse.builder()
                .count(count)
                .likedBy(likedBy)
                .build();
    }

    @Transactional(readOnly = true)
    public long countLikes(Integer announcementId) {
        return likeRepository.countByAnnouncementId(announcementId);
    }

    @Transactional(readOnly = true)
    public boolean isLikedByUser(Integer announcementId, Integer userId) {
        if (userId == null) return false;
        return likeRepository.existsByAnnouncementIdAndUserId(announcementId, userId);
    }
}
