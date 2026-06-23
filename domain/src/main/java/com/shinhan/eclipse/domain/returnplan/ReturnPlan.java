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
@Table(name = "return_plans", uniqueConstraints = @UniqueConstraint(columnNames = "subscription_id"))
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

    public boolean isExecuted() {
        return "EXECUTED".equals(this.planStatus);
    }

    /** 사용자가 "확정" 버튼을 눌렀다는 표시. 상태 전이는 없고 시각만 기록한다 — 이후에도 비율 수정 가능. */
    public void confirm() {
        if (!isDraft()) {
            throw new BusinessException(ErrorCode.RETURN_PLAN_CONFLICT);
        }
        this.confirmedAt = LocalDateTime.now();
    }

    /** 환불일 배치가 호출하는 실제 분배 실행. 사용자 액션이 아니다. */
    public void execute() {
        if (!isDraft()) {
            throw new BusinessException(ErrorCode.RETURN_PLAN_CONFLICT);
        }
        this.executedAt = LocalDateTime.now();
        this.planStatus = "EXECUTED";
    }
}
