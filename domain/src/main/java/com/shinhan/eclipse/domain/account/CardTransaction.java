package com.shinhan.eclipse.domain.account;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "card_transactions")
public class CardTransaction extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long cardId;

    @Column(nullable = false, length = 100)
    private String merchantName;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(nullable = false)
    private LocalDateTime transactedAt;

    // 결제 시점 환율 (절약 금액 계산용)
    @Column(precision = 18, scale = 4)
    private BigDecimal baseRateAtTime;

    @Column(precision = 18, scale = 4)
    private BigDecimal ttsAtTime;

    public static CardTransaction of(Long userId, Long cardId, String merchantName,
                                     String category, BigDecimal amount, LocalDateTime transactedAt,
                                     BigDecimal baseRateAtTime, BigDecimal ttsAtTime) {
        CardTransaction tx = new CardTransaction();
        tx.userId          = userId;
        tx.cardId          = cardId;
        tx.merchantName    = merchantName;
        tx.category        = category;
        tx.amount          = amount;
        tx.transactedAt    = transactedAt;
        tx.baseRateAtTime  = baseRateAtTime;
        tx.ttsAtTime       = ttsAtTime;
        return tx;
    }
}
