package com.shinhan.eclipse.domain.returnplan;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "return_plans")
public class ReturnPlan extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long subscriptionId;

    private Long nextIpoId;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal totalRefundAmount;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(precision = 18, scale = 4)
    private BigDecimal currentSecuritiesBalance;

    @Column(precision = 7, scale = 4)
    private BigDecimal savingsInterestRate;

    @Column(nullable = false, length = 30)
    private String planStatus = "DRAFT";

    private LocalDateTime confirmedAt;
    private LocalDateTime executedAt;
}
