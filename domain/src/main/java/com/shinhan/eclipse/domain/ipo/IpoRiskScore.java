package com.shinhan.eclipse.domain.ipo;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "ipo_risk_scores")
public class IpoRiskScore extends BaseEntity {

    @Column(nullable = false)
    private Long ipoId;

    // 숫자형 위험 점수 — 등급 문자(A/B/C) 저장 금지 (투자자문업 규제 회피)
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Column(length = 255)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String reason;

    private LocalDateTime analyzedAt;
}
