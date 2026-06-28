package com.shinhan.eclipse.domain.account;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 청약 신청 시 계좌에 잠긴(reserve) 금액 1건. 배정 결과에 따라 SETTLED(실차감) 또는 RELEASED(잠금해제)로 정리된다.
 * 현금 이동(실제 actual balance 변경)은 SETTLED 전환 시점에만 발생한다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "balance_holds")
public class BalanceHold extends BaseEntity {

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private Long subscriptionId;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String holdStatus = "LOCKED";

    private LocalDateTime releasedAt;
    private LocalDateTime settledAt;

    public static BalanceHold lock(Long accountId, Long subscriptionId, BigDecimal amount) {
        BalanceHold hold = new BalanceHold();
        hold.accountId = accountId;
        hold.subscriptionId = subscriptionId;
        hold.amount = amount;
        hold.holdStatus = "LOCKED";
        return hold;
    }

    public boolean isLocked() {
        return "LOCKED".equals(this.holdStatus);
    }

    public void release() {
        this.holdStatus = "RELEASED";
        this.releasedAt = LocalDateTime.now();
    }

    public void settle() {
        this.holdStatus = "SETTLED";
        this.settledAt = LocalDateTime.now();
    }
}
