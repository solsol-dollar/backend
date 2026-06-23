package com.shinhan.eclipse.domain.account;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
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

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    public static FinancialAccount createCmaAccount(Long userId, String accountNumberMasked, String currency) {
        FinancialAccount a = new FinancialAccount();
        a.userId = userId;
        a.accountType = "SECURITIES";
        a.institutionType = "SECURITIES_FIRM";
        a.institutionName = "신한투자증권";
        a.accountName = "CMA 계좌";
        a.accountNumberMasked = accountNumberMasked;
        a.currency = currency;
        a.balance = BigDecimal.ZERO;
        a.linked = true;
        a.linkedAt = LocalDateTime.now();
        a.status = "ACTIVE";
        return a;
    }

    public static FinancialAccount link(Long userId, String accountType, String institutionName, String accountNumberMasked) {
        FinancialAccount a = new FinancialAccount();
        a.userId = userId;
        a.accountType = accountType;
        a.institutionType = "SECURITIES".equals(accountType) ? "SECURITIES_FIRM" : "BANK";
        a.institutionName = institutionName;
        a.accountNumberMasked = accountNumberMasked;
        a.balance = BigDecimal.ZERO;
        a.linked = true;
        a.linkedAt = LocalDateTime.now();
        a.status = "ACTIVE";
        return a;
    }

    public void link() {
        this.linked = true;
        this.linkedAt = LocalDateTime.now();
    }

    public void relink(String institutionName, String accountNumberMasked) {
        this.institutionName = institutionName;
        this.accountNumberMasked = accountNumberMasked;
        this.linked = true;
        this.linkedAt = LocalDateTime.now();
    }

    public void unlink() {
        this.linked = false;
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }

    public void deductBalance(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        this.balance = this.balance.subtract(amount);
    }

    public void addBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
}
