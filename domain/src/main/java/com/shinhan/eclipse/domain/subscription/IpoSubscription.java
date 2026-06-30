package com.shinhan.eclipse.domain.subscription;

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
@Table(name = "ipo_subscriptions")
public class IpoSubscription extends BaseEntity {

    // 청약 요청 필드
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long ipoId;

    @Column(nullable = false)
    private Long securitiesAccountId;

    @Column(nullable = false)
    private Integer requestedShares;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal offerPrice;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal subscriptionAmount;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(nullable = false, length = 30)
    private String subscriptionStatus = "REQUESTED";

    @Column(nullable = false, length = 20)
    private String executionMode = "MOCK";

    @Column(nullable = false)
    private LocalDateTime subscribedAt;

    // 배정 결과 필드 (배정 전 NULL)
    private Integer allocatedShares;

    @Column(precision = 18, scale = 4)
    private BigDecimal allocatedAmount;

    @Column(precision = 18, scale = 4)
    private BigDecimal refundAmount;

    @Column(precision = 7, scale = 4)
    private BigDecimal allocationRate;

    @Column(length = 30)
    private String resultStatus;

    private LocalDateTime confirmedAt;

    @Column(nullable = false)
    private boolean scratchRevealed = false;

    public static IpoSubscription request(Long userId, Long ipoId, Long securitiesAccountId,
                                           Integer shares, BigDecimal offerPrice, BigDecimal subscriptionAmount) {
        if (shares == null || shares <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "shares는 1 이상이어야 합니다.");
        }
        if (offerPrice == null || offerPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "offerPrice는 0보다 커야 합니다.");
        }
        if (subscriptionAmount == null || subscriptionAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "subscriptionAmount는 0보다 커야 합니다.");
        }
        IpoSubscription subscription = new IpoSubscription();
        subscription.userId = userId;
        subscription.ipoId = ipoId;
        subscription.securitiesAccountId = securitiesAccountId;
        subscription.requestedShares = shares;
        subscription.offerPrice = offerPrice;
        subscription.subscriptionAmount = subscriptionAmount;
        subscription.subscribedAt = LocalDateTime.now();
        return subscription;
    }

    public boolean isRequested() {
        return "REQUESTED".equals(this.subscriptionStatus);
    }

    public void confirm() {
        this.subscriptionStatus = "CONFIRMED";
        this.confirmedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.subscriptionStatus = "CANCELLED";
    }

    public boolean isAllocated() {
        return this.resultStatus != null;
    }

    /**
     * @param allocationRatePercent 0~100 사이 퍼센트 값
     * @param heldAmount 청약 시점에 잠긴 실제 금액(증거금). refundAmount = heldAmount - allocatedAmount.
     */
    public void deposit() {
        if (!"COMPLETED".equals(this.resultStatus)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "배정 완료된 청약만 입고 처리할 수 있습니다.");
        }
        this.resultStatus = "DEPOSITED";
    }

    public void revealScratch() {
        this.scratchRevealed = true;
    }

    public void allocate(int allocatedShares, BigDecimal allocationRatePercent, BigDecimal heldAmount) {
        if (!"CONFIRMED".equals(this.subscriptionStatus)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "확정된 청약만 배정할 수 있습니다.");
        }
        if (allocatedShares < 0 || allocatedShares > this.requestedShares) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "배정 수량은 0 이상, 신청 수량 이하여야 합니다.");
        }
        if (allocationRatePercent.compareTo(BigDecimal.ZERO) < 0 || allocationRatePercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "배정률은 0~100 사이여야 합니다.");
        }
        this.allocatedShares = allocatedShares;
        this.allocatedAmount = this.offerPrice.multiply(BigDecimal.valueOf(allocatedShares));
        this.refundAmount = heldAmount.subtract(this.allocatedAmount);
        this.allocationRate = allocationRatePercent;
        this.resultStatus = "COMPLETED";
    }
}
