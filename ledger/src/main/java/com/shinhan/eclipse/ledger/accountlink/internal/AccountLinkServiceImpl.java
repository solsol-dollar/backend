package com.shinhan.eclipse.ledger.accountlink.internal;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.ledger.accountlink.AccountLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class AccountLinkServiceImpl implements AccountLinkService {

    private final FinancialAccountRepository financialAccountRepository;

    @Override
    public List<FinancialAccount> getLinkedAccounts(Long userId) {
        // TODO: 구현
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public FinancialAccount linkAccount(Long userId, String accountType, String institutionName, String accountNumberMasked) {
        // TODO: 구현
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void unlinkAccount(Long userId, Long accountId) {
        // TODO: 구현
        throw new UnsupportedOperationException("TODO");
    }
}
