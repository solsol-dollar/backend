package com.shinhan.eclipse.service.mypage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MyPageAccountsResponse(
        List<AccountItem> accounts,
        List<CardItem> cards
) {
    public record AccountItem(
            Long accountId,
            String accountType,
            String accountName,
            String accountNumberMasked,
            String currency,
            BigDecimal balance,
            BigDecimal reservedBalance,
            BigDecimal availableBalance,
            BigDecimal interestRate,
            LocalDate maturityDate
    ) {}

    public record CardItem(
            Long cardId,
            String cardName,
            String cardNumberMasked,
            String issuerName
    ) {}
}
