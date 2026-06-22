package com.shinhan.eclipse.service.app.auth.dto;

import java.math.BigDecimal;
import java.util.List;

public record OnboardingAccountsResponse(
        List<AccountInfo> accounts,
        List<CardInfo> cards
) {
    public record AccountInfo(
            Long id,
            String accountType,
            String accountName,
            String accountNumberMasked,
            String currency,
            BigDecimal balance
    ) {}

    public record CardInfo(
            Long id,
            String cardName,
            String cardNumberMasked
    ) {}
}
