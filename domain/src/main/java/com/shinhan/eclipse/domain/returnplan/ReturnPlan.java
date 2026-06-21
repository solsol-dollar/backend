package com.shinhan.eclipse.domain.returnplan;

import com.shinhan.eclipse.common.entity.BaseEntity;
import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
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

    public static ReturnPlan create(Long userId, Long subscriptionId, BigDecimal totalRefundAmount,
                                     Long nextIpoId, BigDecimal currentSecuritiesBalance, BigDecimal savingsInterestRate) {
        if (totalRefundAmount == null || totalRefundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.ALLOCATION_NOT_FOUND);
        }
        ReturnPlan plan = new ReturnPlan();
        plan.userId = userId;
        plan.subscriptionId = subscriptionId;
        plan.totalRefundAmount = totalRefundAmount;
        plan.nextIpoId = nextIpoId;
        plan.currentSecuritiesBalance = currentSecuritiesBalance;
        plan.savingsInterestRate = savingsInterestRate;
        return plan;
    }

    public boolean isDraft() {
        return "DRAFT".equals(this.planStatus);
    }

    public void confirm() {
        if (!isDraft()) {
            throw new BusinessException(ErrorCode.RETURN_PLAN_CONFLICT);
        }
        this.planStatus = "CONFIRMED";
        this.confirmedAt = LocalDateTime.now();
    }
}
