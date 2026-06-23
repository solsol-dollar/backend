package com.shinhan.eclipse.ledger.accountlink.dto;

import com.shinhan.eclipse.domain.account.FinancialAccount;

import java.math.BigDecimal;

public record AccountRes(
        Long accountId,
        String accountType,
        String institutionName,
        String accountNumberMasked,
        String accountHolderName,
        String currency,
        BigDecimal balance
) {
    public static AccountRes of(FinancialAccount account, String accountHolderName) {
        return new AccountRes(
                account.getId(),
                account.getAccountType(),
                account.getInstitutionName(),
                account.getAccountNumberMasked(),
                accountHolderName,
                account.getCurrency(),
                account.getBalance());
    }
}
