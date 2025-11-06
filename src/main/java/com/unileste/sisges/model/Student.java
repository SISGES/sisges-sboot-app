package com.unileste.sisges.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "student", schema = "sisges")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "class_id")
    private SchoolClass currentClass;
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User baseData;
    @ManyToOne
    @JoinColumn(name = "responsible_id")
    private StudentResponsible responsible;
}