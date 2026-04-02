package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.announcement.AnnouncementResponse;
import com.unileste.sisges.controller.dto.announcement.CreateAnnouncementRequest;
import com.unileste.sisges.controller.dto.announcement.UpdateAnnouncementRequest;
import com.unileste.sisges.model.Announcement;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.AnnouncementRepository;
import com.unileste.sisges.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementService.class);

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final AnnouncementLikeService announcementLikeService;
    private final AnnouncementCommentService announcementCommentService;

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> findActiveForRole(String role, Integer currentUserId) {
        LocalDateTime now = LocalDateTime.now();
        List<Announcement> announcements;
        try {
            announcements = announcementRepository.findActiveForRole(role, now);
        } catch (Exception e) {
            log.warn("findActiveForRole falhou (coluna hidden_for_roles pode não existir), usando fallback: {}", e.getMessage());
            announcements = announcementRepository.findActive(now).stream()
                    .filter(a -> !a.getHiddenForRolesList().contains(role))
                    .collect(Collectors.toList());
        }
        return announcements.stream()
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
        long likeCount = safeCountLikes(a.getId());
        boolean likedByCurrentUser = safeIsLikedByUser(a.getId(), currentUserId);
        long commentCount = safeCountComments(a.getId());

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

    private long safeCountLikes(Integer announcementId) {
        try {
            return announcementLikeService.countLikes(announcementId);
        } catch (Exception e) {
            log.warn("Erro ao contar likes do anúncio {}: {}", announcementId, e.getMessage());
            return 0;
        }
    }

    private boolean safeIsLikedByUser(Integer announcementId, Integer userId) {
        try {
            return announcementLikeService.isLikedByUser(announcementId, userId);
        } catch (Exception e) {
            log.warn("Erro ao verificar like do usuário {} no anúncio {}: {}", userId, announcementId, e.getMessage());
            return false;
        }
    }

    private long safeCountComments(Integer announcementId) {
        try {
            return announcementCommentService.countComments(announcementId);
        } catch (Exception e) {
            log.warn("Erro ao contar comentários do anúncio {}: {}", announcementId, e.getMessage());
            return 0;
        }
    }
}
