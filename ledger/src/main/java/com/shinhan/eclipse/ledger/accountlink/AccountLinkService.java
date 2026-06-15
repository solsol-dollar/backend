package com.shinhan.eclipse.ledger.accountlink;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import java.util.List;

public interface AccountLinkService {
    List<FinancialAccount> getLinkedAccounts(Long userId);
    FinancialAccount linkAccount(Long userId, String accountType, String institutionName, String accountNumberMasked);
    void unlinkAccount(Long userId, Long accountId);
}
