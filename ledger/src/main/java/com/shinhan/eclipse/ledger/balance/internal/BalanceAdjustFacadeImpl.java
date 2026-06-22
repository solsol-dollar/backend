package com.shinhan.eclipse.ledger.balance.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.ledger.balance.BalanceAdjustFacade;
import com.shinhan.eclipse.ledger.balance.BalanceAdjustRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
class BalanceAdjustFacadeImpl implements BalanceAdjustFacade {

    private final BalanceAccountRepository balanceAccountRepository;

    @Override
    @Transactional
    public BigDecimal adjust(BalanceAdjustRequest request) {
        FinancialAccount account = balanceAccountRepository
                .findByIdAndUserIdForUpdate(request.accountId(), request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if ("DEDUCT".equals(request.type())) {
            account.deductBalance(request.amount());
        } else if ("ADD".equals(request.type())) {
            account.addBalance(request.amount());
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        balanceAccountRepository.save(account);
        return account.getBalance();
    }
}
