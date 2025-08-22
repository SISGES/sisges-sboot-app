package com.unileste.sisges.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "class", schema = "sisges")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClassEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false, unique = true)
    private String name;
    @OneToMany(mappedBy = "classEntity")
    private List<Student> students = new ArrayList<>();
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}