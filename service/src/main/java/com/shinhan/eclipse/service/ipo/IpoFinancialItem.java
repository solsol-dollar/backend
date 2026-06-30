package com.shinhan.eclipse.service.ipo;

public record IpoFinancialItem(
        int fiscalYear,
        String currency,
        java.math.BigDecimal exchangeRate,
        String revenue,
        String operatingIncome,
        String netIncome,
        String revenueKrw,
        String operatingIncomeKrw,
        String netIncomeKrw
) {}
