package com.shinhan.eclipse.domain.ipo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "ipo_score")
public class IpoScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ipoId;

    @Column(length = 20)
    private String ticker;

    @Column(nullable = false)
    private Integer finalScore;

    @Column(nullable = false, length = 20)
    private String grade;

    @Column(length = 500)
    private String reason;

    @Column(columnDefinition = "JSON")
    private String topNewsIds;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private Integer newsCount;

    private LocalDateTime scoredAt;

    private Integer postFinalScore;

    @Column(length = 30)
    private String postGrade;

    @Column(length = 500)
    private String postReason;

    @Column(columnDefinition = "JSON")
    private String postTopNewsIds;

    @Column(columnDefinition = "TEXT")
    private String postSummary;

    private Integer postNewsCount;

    private LocalDateTime postScoredAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
