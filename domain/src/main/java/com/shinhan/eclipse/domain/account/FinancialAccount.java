package com.shinhan.eclipse.domain.account;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "financial_accounts")
public class FinancialAccount extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String accountType;

    @Column(nullable = false, length = 30)
    private String institutionType;

    @Column(nullable = false, length = 50)
    private String institutionName;

    @Column(length = 100)
    private String accountName;

    @Column(length = 50)
    private String accountNumberMasked;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal balance;

    @Column(precision = 7, scale = 4)
    private BigDecimal interestRate;

    private LocalDate maturityDate;

    @Column(nullable = false)
    private Boolean linked;

    private LocalDateTime linkedAt;

    public void deductBalance(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) throw new IllegalStateException("잔액 부족");
        this.balance = this.balance.subtract(amount);
    }

    public void addBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
}
