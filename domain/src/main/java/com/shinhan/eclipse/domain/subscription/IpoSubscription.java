package com.shinhan.eclipse.domain.subscription;

import com.shinhan.eclipse.common.entity.BaseEntity;
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

    public static IpoSubscription request(Long userId, Long ipoId, Long securitiesAccountId,
                                           Integer shares, BigDecimal offerPrice) {
        IpoSubscription subscription = new IpoSubscription();
        subscription.userId = userId;
        subscription.ipoId = ipoId;
        subscription.securitiesAccountId = securitiesAccountId;
        subscription.requestedShares = shares;
        subscription.offerPrice = offerPrice;
        subscription.subscriptionAmount = offerPrice.multiply(BigDecimal.valueOf(shares));
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
}
