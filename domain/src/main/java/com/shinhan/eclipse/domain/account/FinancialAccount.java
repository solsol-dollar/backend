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

    public static FinancialAccount createDepositAccount(Long userId) {
        FinancialAccount a = new FinancialAccount();
        a.userId = userId;
        a.accountType = "DEPOSIT";
        a.institutionType = "BANK";
        a.institutionName = "신한은행";
        a.accountName = "외화 체인지업 예금";
        a.accountNumberMasked = generateMaskedAccountNumber();
        a.currency = "USD";
        a.balance = BigDecimal.ZERO;
        a.interestRate = new BigDecimal("3.00");
        a.maturityDate = LocalDate.now().plusYears(1);
        a.linked = true;
        a.linkedAt = LocalDateTime.now();
        a.status = "ACTIVE";
        return a;
    }

    public static FinancialAccount createSavingsAccount(Long userId) {
        FinancialAccount a = new FinancialAccount();
        a.userId = userId;
        a.accountType = "SAVINGS";
        a.institutionType = "BANK";
        a.institutionName = "신한은행";
        a.accountName = "신한 Value-up 외화적립예금";
        a.accountNumberMasked = generateMaskedAccountNumber();
        a.currency = "USD";
        a.balance = BigDecimal.ZERO;
        a.interestRate = new BigDecimal("4.00");
        a.maturityDate = LocalDate.now().plusYears(1);
        a.linked = true;
        a.linkedAt = LocalDateTime.now();
        a.status = "ACTIVE";
        return a;
    }

    private static String generateMaskedAccountNumber() {
        int suffix = (int) (Math.random() * 9000) + 1000;
        return "****-****-" + suffix;
    }

    public void link() {
        this.linked = true;
        this.linkedAt = LocalDateTime.now();
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
