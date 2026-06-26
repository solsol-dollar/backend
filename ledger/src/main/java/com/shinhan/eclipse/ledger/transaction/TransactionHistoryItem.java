package com.shinhan.eclipse.ledger.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionHistoryItem(
        Long id,
        String type,             // IN, OUT, CARD, EXCHANGE
        BigDecimal amount,
        String currency,
        String status,
        LocalDateTime executedAt,

        // 송금 상대방 정보
        AccountInfo fromAccount,
        AccountInfo toAccount,

        // 환전 전용
        String fromCurrency,
        String toCurrency,
        BigDecimal exchangeRate,
        BigDecimal sourceAmount,
        BigDecimal targetAmount,

        // 카드 결제 전용
        String description
) {
    public record AccountInfo(
            Long accountId,
            String accountName,
            String accountNumberMasked,
            String institutionName
    ) {}

    public static TransactionHistoryItem ofTransfer(Long id, String type,
            BigDecimal amount, String currency, String status, LocalDateTime executedAt,
            AccountInfo fromAccount, AccountInfo toAccount) {
        return new TransactionHistoryItem(id, type, amount, currency, status, executedAt,
                fromAccount, toAccount, null, null, null, null, null, null);
    }

    public static TransactionHistoryItem ofCard(Long id,
            BigDecimal amount, String currency, String status, LocalDateTime executedAt,
            AccountInfo fromAccount, String description) {
        return new TransactionHistoryItem(id, "CARD", amount, currency, status, executedAt,
                fromAccount, null, null, null, null, null, null, description);
    }

    public static TransactionHistoryItem ofExchange(Long id,
            String fromCurrency, String toCurrency, BigDecimal exchangeRate,
            BigDecimal sourceAmount, BigDecimal targetAmount,
            String status, LocalDateTime executedAt,
            AccountInfo fromAccount, AccountInfo toAccount) {
        return new TransactionHistoryItem(id, "EXCHANGE",
                sourceAmount, fromCurrency, status, executedAt,
                fromAccount, toAccount,
                fromCurrency, toCurrency, exchangeRate, sourceAmount, targetAmount, null);
    }
}
