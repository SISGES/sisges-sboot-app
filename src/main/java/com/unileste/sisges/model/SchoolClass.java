package com.unileste.sisges.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "school_class", schema = "sisges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "academic_year", nullable = false, length = 9)
    private String academicYear;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "currentClass")
    @Builder.Default
    private List<Student> students = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "teacher_class",
        schema = "sisges",
        joinColumns = @JoinColumn(name = "class_id"),
        inverseJoinColumns = @JoinColumn(name = "teacher_id")
    )
    @Builder.Default
    private List<Teacher> teachers = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "class_discipline",
        schema = "sisges",
        joinColumns = @JoinColumn(name = "class_id"),
        inverseJoinColumns = @JoinColumn(name = "discipline_id")
    )
    @Builder.Default
    private List<Discipline> disciplines = new ArrayList<>();

    @OneToMany(mappedBy = "schoolClass")
    @Builder.Default
    private List<ClassMeeting> meetings = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
