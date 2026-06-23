package com.shinhan.eclipse.service.home;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AssetsSummaryResponse(
        SecuritiesAsset securities,
        List<AccountAsset> accounts,
        List<CardAsset> cards,
        ExchangeRateInfo exchangeRateInfo,
        BigDecimal totalUsdBalance
) {

    public record ExchangeRateInfo(
            BigDecimal rate,
            BigDecimal previousRate,
            BigDecimal changeAmount,
            BigDecimal changeRate
    ) {}

    public record SecuritiesAsset(
            String accountNumberMasked,
            BigDecimal usdBalance,
            BigDecimal krwBalance,
            BigDecimal totalUsdBalance
    ) {}

    public record AccountAsset(
            String accountType,
            String accountName,
            String accountNumberMasked,
            BigDecimal balance,
            BigDecimal interestRate,
            LocalDate maturityDate
    ) {}

    public record CardAsset(
            String cardName,
            String cardNumberMasked,
            String issuerName
    ) {}
}