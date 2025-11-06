package com.unileste.sisges.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponsible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String phone;
    @Column(nullable = false)
    private String alternativePhone;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String alternativeEmail;
    @OneToMany(mappedBy = "responsible")
    private List<Student> students;
}