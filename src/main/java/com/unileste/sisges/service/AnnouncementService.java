package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.announcement.AnnouncementResponse;
import com.unileste.sisges.controller.dto.announcement.CreateAnnouncementRequest;
import com.unileste.sisges.controller.dto.announcement.UpdateAnnouncementRequest;
import com.unileste.sisges.model.Announcement;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.AnnouncementRepository;
import com.unileste.sisges.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final AnnouncementLikeService announcementLikeService;
    private final AnnouncementCommentService announcementCommentService;

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> findActiveForRole(String role, Integer currentUserId) {
        LocalDateTime now = LocalDateTime.now();
        return announcementRepository.findActiveForRole(role, now).stream()
                .map(a -> toResponse(a, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> findAll(Integer currentUserId) {
        return announcementRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(a -> toResponse(a, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AnnouncementResponse findById(Integer id, Integer currentUserId) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Anúncio não encontrado"));
        if (a.getDeletedAt() != null) {
            throw new RuntimeException("Anúncio não encontrado");
        }
        return toResponse(a, currentUserId);
    }

    @Transactional
    public AnnouncementResponse create(CreateAnnouncementRequest request, Integer createdById) {
        User createdBy = createdById != null ? userRepository.findById(createdById).orElse(null) : null;
        String hiddenForRolesStr = request.getHiddenForRoles() != null && !request.getHiddenForRoles().isEmpty()
                ? String.join(",", request.getHiddenForRoles())
                : null;

        Announcement announcement = Announcement.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .type(request.getType())
                .imagePath(request.getImagePath())
                .hiddenForRoles(hiddenForRolesStr)
                .activeFrom(request.getActiveFrom())
                .activeUntil(request.getActiveUntil())
                .createdBy(createdBy)
                .build();

        announcement = announcementRepository.save(announcement);
        return toResponse(announcement, createdById);
    }

    @Transactional
    public AnnouncementResponse update(Integer id, UpdateAnnouncementRequest request) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Anúncio não encontrado"));
        if (a.getDeletedAt() != null) {
            throw new RuntimeException("Anúncio não encontrado");
        }

        if (request.getTitle() != null) a.setTitle(request.getTitle());
        if (request.getContent() != null) a.setContent(request.getContent());
        if (request.getType() != null) a.setType(request.getType());
        if (request.getImagePath() != null) a.setImagePath(request.getImagePath());
        if (request.getTargetRoles() != null) {
            a.setTargetRoles(String.join(",", request.getTargetRoles()));
        }
        if (request.getHiddenForRoles() != null) {
            a.setHiddenForRoles(request.getHiddenForRoles().isEmpty()
                    ? null : String.join(",", request.getHiddenForRoles()));
        }
        if (request.getActiveFrom() != null) a.setActiveFrom(request.getActiveFrom());
        if (request.getActiveUntil() != null) a.setActiveUntil(request.getActiveUntil());

        a = announcementRepository.save(a);
        return toResponse(a, null);
    }

    @Transactional
    public void delete(Integer id) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Anúncio não encontrado"));
        a.setDeletedAt(LocalDateTime.now());
        announcementRepository.save(a);
    }

    private AnnouncementResponse toResponse(Announcement a, Integer currentUserId) {
        long likeCount = announcementLikeService.countLikes(a.getId());
        boolean likedByCurrentUser = announcementLikeService.isLikedByUser(a.getId(), currentUserId);
        long commentCount = announcementCommentService.countComments(a.getId());

        return AnnouncementResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .type(a.getType())
                .imagePath(a.getImagePath())
                .targetRoles(a.getTargetRolesList())
                .hiddenForRoles(a.getHiddenForRolesList())
                .activeFrom(a.getActiveFrom())
                .activeUntil(a.getActiveUntil())
                .createdAt(a.getCreatedAt())
                .likeCount(likeCount)
                .likedByCurrentUser(likedByCurrentUser)
                .commentCount(commentCount)
                .build();
    }
}
