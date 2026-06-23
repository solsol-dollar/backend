package com.shinhan.eclipse.domain.returnplan;

import com.shinhan.eclipse.common.entity.BaseEntity;
import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "return_plan_allocations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"return_plan_id", "destination_type"}))
public class ReturnPlanAllocation extends BaseEntity {

    @Column(nullable = false)
    private Long returnPlanId;

    @Column(nullable = false, length = 30)
    private String destinationType;

    private Long destinationAccountId;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal allocationRatio;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal allocationAmount;

    @Column(nullable = false, length = 30)
    private String allocationStatus = "PENDING";

    public static final Set<String> DESTINATION_TYPES = Set.of("SECURITIES", "SAVINGS", "DEPOSIT");

    public static ReturnPlanAllocation initZero(Long returnPlanId, String destinationType) {
        validateDestinationType(destinationType);
        ReturnPlanAllocation allocation = new ReturnPlanAllocation();
        allocation.returnPlanId = returnPlanId;
        allocation.destinationType = destinationType;
        allocation.allocationRatio = BigDecimal.ZERO;
        allocation.allocationAmount = BigDecimal.ZERO;
        return allocation;
    }

    public void updateRatio(Integer ratio, BigDecimal totalRefundAmount) {
        if (ratio == null || ratio < 0 || ratio > 100 || ratio % 5 != 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "비율은 0~100 사이 5단위여야 합니다.");
        }
        this.allocationRatio = BigDecimal.valueOf(ratio);
        this.allocationAmount = totalRefundAmount
                .multiply(BigDecimal.valueOf(ratio))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    /** 실제 자금 이동 완료 후, 적립된 계좌와 상태를 기록한다. */
    public void markExecuted(Long destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
        this.allocationStatus = "EXECUTED";
    }

    private static void validateDestinationType(String destinationType) {
        if (destinationType == null || !DESTINATION_TYPES.contains(destinationType)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "지원하지 않는 분배 대상입니다: " + destinationType);
        }
    }
}
