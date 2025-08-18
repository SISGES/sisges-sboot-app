package com.unileste.sisges.model;

import com.unileste.sisges.enums.converter.GenderENUM;
import com.unileste.sisges.enums.converter.GenderEnumConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "student", schema = "sisges")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false, unique = true)
    private String register;
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    @Column(name = "responsible1_name", nullable = false, length = 100)
    private String responsible1Name;
    @Column(name = "responsible1_phone", nullable = false, length = 15)
    private String responsible1Phone;
    @Column(name = "responsible1_email", nullable = false, length = 254)
    private String responsible1Email;
    @Column(name = "responsible2_name", length = 100)
    private String responsible2Name;
    @Column(name = "responsible2_phone", length = 15)
    private String responsible2Phone;
    @Column(name = "responsible2_email", length = 254)
    private String responsible2Email;
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;
    @Convert(converter = GenderEnumConverter.class)
    @Column(name = "gender_id", length = 2)
    private GenderENUM gender;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}