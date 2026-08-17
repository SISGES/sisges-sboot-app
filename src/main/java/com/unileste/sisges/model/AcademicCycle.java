package com.unileste.sisges.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "academic_cycle", schema = "sisges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AcademicCycleStatus status;

    @Column(name = "year_start_date")
    private LocalDate yearStartDate;

    @Column(name = "year_end_date")
    private LocalDate yearEndDate;

    @Column(name = "current_trimester", nullable = false)
    private Integer currentTrimester;

    @Column(name = "grading_locked", nullable = false)
    private boolean gradingLocked;

    @Column(name = "year_started_at")
    private LocalDateTime yearStartedAt;

    @Column(name = "year_finished_at")
    private LocalDateTime yearFinishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = AcademicCycleStatus.NOT_STARTED;
        }
        if (currentTrimester == null) {
            currentTrimester = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

