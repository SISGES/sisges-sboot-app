package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.announcement.*;
import com.unileste.sisges.security.UserPrincipal;
import com.unileste.sisges.service.AnnouncementCommentService;
import com.unileste.sisges.service.AnnouncementLikeService;
import com.unileste.sisges.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementController.class);

    private final AnnouncementService announcementService;
    private final AnnouncementLikeService announcementLikeService;
    private final AnnouncementCommentService announcementCommentService;

    @GetMapping("/feed")
    public ResponseEntity<List<AnnouncementResponse>> getFeed(@AuthenticationPrincipal UserPrincipal principal) {
        String role = principal != null ? principal.getRole() : "STUDENT";
        Integer currentUserId = principal != null ? principal.getId() : null;
        List<AnnouncementResponse> feed = announcementService.findActiveForRole(role, currentUserId);
        return ResponseEntity.ok(feed);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AnnouncementResponse>> findAll(@AuthenticationPrincipal UserPrincipal principal) {
        Integer currentUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(announcementService.findAll(currentUserId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnnouncementResponse> findById(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Integer currentUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(announcementService.findById(id, currentUserId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<AnnouncementResponse> create(
            @Valid @RequestBody CreateAnnouncementRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Integer createdById = principal != null ? principal.getId() : null;
        AnnouncementResponse response = announcementService.create(request, createdById);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnnouncementResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateAnnouncementRequest request) {
        return ResponseEntity.ok(announcementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        announcementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ToggleLikeResponse> toggleLike(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Integer userId = principal != null ? principal.getId() : null;
        boolean liked = announcementLikeService.toggleLike(id, userId);
        return ResponseEntity.ok(ToggleLikeResponse.builder().liked(liked).build());
    }

    @GetMapping("/{id}/likes")
    public ResponseEntity<AnnouncementLikesResponse> getLikes(@PathVariable Integer id) {
        return ResponseEntity.ok(announcementLikeService.getLikes(id));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnnouncementCommentResponse> addComment(
            @PathVariable Integer id,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Integer userId = principal != null ? principal.getId() : null;
        AnnouncementCommentResponse response = announcementCommentService.addComment(id, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<AnnouncementCommentResponse>> getComments(@PathVariable Integer id) {
        return ResponseEntity.ok(announcementCommentService.getComments(id));
    }

    @PutMapping("/{id}/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnnouncementCommentResponse> updateComment(
            @PathVariable Integer id,
            @PathVariable Integer commentId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Integer userId = principal != null ? principal.getId() : null;
        AnnouncementCommentResponse response = announcementCommentService.updateComment(id, commentId, userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Integer id,
            @PathVariable Integer commentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Integer userId = principal != null ? principal.getId() : null;
        announcementCommentService.deleteComment(id, commentId, userId);
        return ResponseEntity.noContent().build();
    }
}
