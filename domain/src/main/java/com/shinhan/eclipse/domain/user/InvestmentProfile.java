package com.shinhan.eclipse.domain.user;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "investment_profiles")
public class InvestmentProfile extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String riskType;

    @Column(nullable = false)
    private Integer totalScore;

    private Integer investmentExperienceScore;
    private Integer returnExpectationScore;
    private Integer lossToleranceScore;

    @Column(precision = 5, scale = 2)
    private BigDecimal recommendedIpoRatio;

    @Column(precision = 5, scale = 2)
    private BigDecimal recommendedEtfRatio;

    @Column(precision = 5, scale = 2)
    private BigDecimal recommendedSavingsRatio;

    @Column(precision = 5, scale = 2)
    private BigDecimal recommendedRpRatio;

    @Column(nullable = false)
    private LocalDateTime diagnosedAt;
}
