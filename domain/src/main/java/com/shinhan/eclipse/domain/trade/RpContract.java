package com.shinhan.eclipse.domain.trade;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "rp_contracts")
public class RpContract extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal principalAmount;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal interestRate;

    @Column(precision = 18, scale = 4)
    private BigDecimal expectedInterest;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime maturityAt;

    @Column(nullable = false, length = 30)
    private String contractStatus = "ACTIVE";

    @Column(nullable = false, length = 20)
    private String executionMode = "MOCK";
}
