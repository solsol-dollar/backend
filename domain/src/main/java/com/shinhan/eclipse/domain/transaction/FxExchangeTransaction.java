package com.shinhan.eclipse.domain.transaction;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "fx_exchange_transactions")
public class FxExchangeTransaction extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    private Long fromAccountId;
    private Long toAccountId;

    @Column(nullable = false, length = 10)
    private String fromCurrency = "KRW";

    @Column(nullable = false, length = 10)
    private String toCurrency = "USD";

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal exchangeRate;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal sourceAmount;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal targetAmount;

    @Column(precision = 18, scale = 4)
    private BigDecimal fromBalanceAfter;

    @Column(precision = 18, scale = 4)
    private BigDecimal toBalanceAfter;

    @Column(nullable = false, length = 30)
    private String exchangeStatus = "REQUESTED";

    @Column(nullable = false, length = 20)
    private String executionMode = "MOCK";

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime completedAt;

    @Column(length = 255)
    private String failureReason;
}
