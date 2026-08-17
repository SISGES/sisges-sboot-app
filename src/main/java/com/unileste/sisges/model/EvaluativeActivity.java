package com.unileste.sisges.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "evaluative_activity", schema = "sisges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluativeActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_meeting_id", nullable = false)
    private ClassMeeting classMeeting;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "trimester_number")
    private Integer trimesterNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 40)
    @Builder.Default
    private ActivityType activityType = ActivityType.ATIVIDADE;

    @Column(name = "max_points", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal maxPoints = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private boolean released = false;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "released_by_user_id")
    private User releasedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
