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
    private String accountNumber;

    @Column(length = 50)
    private String virtualAccountNumber;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal balance;

    /** 청약 등으로 잠긴 금액(홀딩). 실제 현금은 빠져나가지 않은 상태. */
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal reservedBalance = BigDecimal.ZERO;

    @Column(precision = 7, scale = 4)
    private BigDecimal interestRate;

    private LocalDate maturityDate;

    @Column(nullable = false)
    private Boolean linked;

    private LocalDateTime linkedAt;

    public static FinancialAccount createCmaAccount(Long userId, String accountNumber, String currency) {
        FinancialAccount a = new FinancialAccount();
        a.userId = userId;
        a.accountType = "SECURITIES";
        a.institutionType = "SECURITIES_FIRM";
        a.institutionName = "신한투자증권";
        a.accountName = "CMA 계좌";
        a.accountNumber = accountNumber;
        a.currency = currency;
        a.balance = BigDecimal.ZERO;
        a.linked = true;
        a.linkedAt = LocalDateTime.now();
        return a;
    }

    public static FinancialAccount createDepositAccount(Long userId) {
        FinancialAccount a = new FinancialAccount();
        a.userId = userId;
        a.accountType = "DEPOSIT";
        a.institutionType = "BANK";
        a.institutionName = "신한은행";
        a.accountName = "외화 체인지업 예금";
        a.accountNumber = generateAccountNumber();
        a.currency = "USD";
        a.balance = BigDecimal.ZERO;
        a.interestRate = new BigDecimal("3.00");
        a.maturityDate = LocalDate.now().plusYears(1);
        a.linked = true;
        a.linkedAt = LocalDateTime.now();
        return a;
    }

    public static FinancialAccount createSavingsAccount(Long userId) {
        FinancialAccount a = new FinancialAccount();
        a.userId = userId;
        a.accountType = "SAVINGS";
        a.institutionType = "BANK";
        a.institutionName = "신한은행";
        a.accountName = "신한 Value-up 외화적립예금";
        a.accountNumber = generateAccountNumber();
        a.currency = "USD";
        a.balance = BigDecimal.ZERO;
        a.interestRate = new BigDecimal("4.00");
        a.maturityDate = LocalDate.now().plusYears(1);
        a.linked = true;
        a.linkedAt = LocalDateTime.now();
        return a;
    }

    private static String generateAccountNumber() {
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

    /** 실제 잔액 중 지금 바로 신청 등에 쓸 수 있는 금액 (= 실제잔액 - 홀딩된 금액). */
    public BigDecimal availableBalance() {
        return this.balance.subtract(this.reservedBalance);
    }

    /** 청약 신청 등으로 금액을 잠근다. 현금은 그대로, reservedBalance만 늘어난다. */
    public void reserve(BigDecimal amount) {
        if (availableBalance().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.reservedBalance = this.reservedBalance.add(amount);
    }

    /** 미배정/취소 등으로 잠금을 해제한다. 현금 이동 없음. */
    public void releaseReserved(BigDecimal amount) {
        this.reservedBalance = this.reservedBalance.subtract(amount);
    }

    /** 배정 확정분만큼 잠금을 풀면서 동시에 실제 차감한다 (홀딩 → 실차감 전환). */
    public void settleReserved(BigDecimal amount) {
        if (this.reservedBalance.compareTo(amount) < 0 || this.balance.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.reservedBalance = this.reservedBalance.subtract(amount);
        this.balance = this.balance.subtract(amount);
    }
}
