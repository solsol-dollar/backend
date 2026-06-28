package com.shinhan.eclipse.service.ipo;

public record IpoFinancialItem(
        int fiscalYear,
        Long revenue,
        Long operatingIncome,
        Long netIncome,
        String currency,
        Long revenueKrw,
        Long operatingIncomeKrw,
        Long netIncomeKrw
) {}
