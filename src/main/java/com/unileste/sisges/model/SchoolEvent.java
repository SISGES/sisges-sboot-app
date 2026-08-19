package com.unileste.sisges.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "school_event", schema = "sisges")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SchoolEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "event_at", nullable = false) private LocalDateTime eventAt;
    @Column(nullable = false) private String audience;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "class_id") private SchoolClass schoolClass;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by", nullable = false) private User createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
