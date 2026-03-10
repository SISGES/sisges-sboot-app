package com.unileste.sisges.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "announcement", schema = "sisges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String type = "TEXT";

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "target_roles", length = 100)
    private String targetRoles;

    @Column(name = "hidden_for_roles", length = 100)
    private String hiddenForRoles;

    @Column(name = "active_from")
    private LocalDateTime activeFrom;

    @Column(name = "active_until")
    private LocalDateTime activeUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public List<String> getTargetRolesList() {
        if (targetRoles == null || targetRoles.isBlank()) return List.of();
        return Arrays.stream(targetRoles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public List<String> getHiddenForRolesList() {
        if (hiddenForRoles == null || hiddenForRoles.isBlank()) return List.of();
        return Arrays.stream(hiddenForRoles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
