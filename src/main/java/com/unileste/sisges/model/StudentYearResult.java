package com.unileste.sisges.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_year_result", schema = "sisges",
        uniqueConstraints = @UniqueConstraint(columnNames = {"academic_cycle_id", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentYearResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_cycle_id", nullable = false)
    private AcademicCycle academicCycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_class_id", nullable = false)
    private SchoolClass sourceClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_class_id")
    private SchoolClass nextClass;

    @Column(name = "base_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal baseScore;

    @Column(name = "final_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "year_recovery_score", precision = 8, scale = 2)
    private BigDecimal yearRecoveryScore;

    @Column(nullable = false)
    private boolean approved;

    @Column(nullable = false)
    private boolean promoted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

