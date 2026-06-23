package com.shinhan.eclipse.ledger.accountlink.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.ledger.accountlink.AccountLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
    @Transactional(readOnly = true)
    public Optional<FinancialAccount> findAccountByType(Long userId, String accountType) {
        return financialAccountRepository.findFirstByUserIdAndAccountTypeAndLinkedTrueOrderByIdAsc(userId, accountType);
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

    @Override
    @Transactional(readOnly = true)
    public FinancialAccount getLinkedAccount(Long userId, Long accountId) {
        FinancialAccount account = financialAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED));
        if (!Boolean.TRUE.equals(account.getLinked())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED);
        }
        return account;
    }

    @Override
    @Transactional
    public FinancialAccount lockAccount(Long userId, Long accountId) {
        FinancialAccount account = financialAccountRepository.findByIdAndUserIdForUpdate(accountId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED));
        if (!Boolean.TRUE.equals(account.getLinked())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED);
        }
        return account;
    }

    @Override
    @Transactional
    public void deduct(Long userId, Long accountId, BigDecimal amount) {
        FinancialAccount account = financialAccountRepository.findByIdAndUserIdForUpdate(accountId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED));
        if (!Boolean.TRUE.equals(account.getLinked())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED);
        }
        account.deductBalance(amount);
    }

    @Override
    @Transactional
    public void credit(Long userId, Long accountId, BigDecimal amount) {
        FinancialAccount account = financialAccountRepository.findByIdAndUserIdForUpdate(accountId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED));
        if (!Boolean.TRUE.equals(account.getLinked())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED);
        }
        account.addBalance(amount);
    }
}
