package com.unileste.sisges.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "teacher", schema = "sisges")
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToMany
    @JoinTable(
            name = "teacher_class",
            joinColumns = @JoinColumn(name = "teacher_id"),
            inverseJoinColumns = @JoinColumn(name = "class_id")
    )
    private List<SchoolClass> classes = new ArrayList<>();
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User baseData;

    public void addClass(SchoolClass schoolClass) {
        this.classes.add(schoolClass);
    }

    public void removeClass(SchoolClass schoolClass) {
        this.classes.remove(schoolClass);
        schoolClass.getTeachers().remove(this);
    }
}