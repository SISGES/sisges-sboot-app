package com.unileste.sisges.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "grading_config", schema = "sisges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "year_max_points", nullable = false)
    private Integer yearMaxPoints;

    @Column(name = "year_min_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal yearMinPercentage;

    @Column(name = "trimester1_max_points", nullable = false)
    private Integer trimester1MaxPoints;

    @Column(name = "trimester1_min_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal trimester1MinPercentage;

    @Column(name = "trimester2_max_points", nullable = false)
    private Integer trimester2MaxPoints;

    @Column(name = "trimester2_min_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal trimester2MinPercentage;

    @Column(name = "trimester3_max_points", nullable = false)
    private Integer trimester3MaxPoints;

    @Column(name = "trimester3_min_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal trimester3MinPercentage;

    @Column(name = "trimester1_points_provas", nullable = false)
    private Integer trimester1PointsProvas;

    @Column(name = "trimester1_points_atividades", nullable = false)
    private Integer trimester1PointsAtividades;

    @Column(name = "trimester1_points_trabalhos", nullable = false)
    private Integer trimester1PointsTrabalhos;

    @Column(name = "trimester2_points_provas", nullable = false)
    private Integer trimester2PointsProvas;

    @Column(name = "trimester2_points_atividades", nullable = false)
    private Integer trimester2PointsAtividades;

    @Column(name = "trimester2_points_trabalhos", nullable = false)
    private Integer trimester2PointsTrabalhos;

    @Column(name = "trimester3_points_provas", nullable = false)
    private Integer trimester3PointsProvas;

    @Column(name = "trimester3_points_atividades", nullable = false)
    private Integer trimester3PointsAtividades;

    @Column(name = "trimester3_points_trabalhos", nullable = false)
    private Integer trimester3PointsTrabalhos;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
