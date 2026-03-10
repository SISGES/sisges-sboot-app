package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.announcement.AnnouncementCommentResponse;
import com.unileste.sisges.controller.dto.announcement.CreateCommentRequest;
import com.unileste.sisges.controller.dto.announcement.UserSimpleResponse;
import com.unileste.sisges.model.Announcement;
import com.unileste.sisges.model.AnnouncementComment;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.AnnouncementCommentRepository;
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
public class AnnouncementCommentService {

    private final AnnouncementCommentRepository commentRepository;
    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    @Transactional
    public AnnouncementCommentResponse addComment(Integer announcementId, Integer userId, CreateCommentRequest request) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Anúncio não encontrado"));
        if (announcement.getDeletedAt() != null) {
            throw new RuntimeException("Anúncio não encontrado");
        }
        if (userId == null) {
            throw new RuntimeException("Usuário não autenticado");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        AnnouncementComment comment = AnnouncementComment.builder()
                .announcement(announcement)
                .user(user)
                .content(request.getContent().trim())
                .build();

        comment = commentRepository.save(comment);
        return toResponse(comment);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementCommentResponse> getComments(Integer announcementId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Anúncio não encontrado"));
        if (announcement.getDeletedAt() != null) {
            throw new RuntimeException("Anúncio não encontrado");
        }

        return commentRepository.findByAnnouncementIdAndDeletedAtIsNullOrderByCreatedAtAsc(announcementId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(Integer announcementId, Integer commentId, Integer userId) {
        AnnouncementComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comentário não encontrado"));
        if (comment.getDeletedAt() != null) {
            throw new RuntimeException("Comentário não encontrado");
        }
        if (!comment.getAnnouncement().getId().equals(announcementId)) {
            throw new RuntimeException("Comentário não pertence a este anúncio");
        }
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Apenas o autor pode excluir o comentário");
        }

        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public long countComments(Integer announcementId) {
        return commentRepository.countByAnnouncementIdAndDeletedAtIsNull(announcementId);
    }

    private AnnouncementCommentResponse toResponse(AnnouncementComment c) {
        return AnnouncementCommentResponse.builder()
                .id(c.getId())
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .user(UserSimpleResponse.builder()
                        .id(c.getUser().getId())
                        .name(c.getUser().getName())
                        .build())
                .build();
    }
}
