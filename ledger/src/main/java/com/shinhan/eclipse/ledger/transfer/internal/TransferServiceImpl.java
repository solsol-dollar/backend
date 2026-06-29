package com.shinhan.eclipse.ledger.transfer.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.transaction.TransferTransaction;
import com.shinhan.eclipse.ledger.transfer.TransferRequest;
import com.shinhan.eclipse.ledger.transfer.TransferResult;
import com.shinhan.eclipse.ledger.transfer.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class TransferServiceImpl implements TransferService {

    private static final String SAVINGS    = "SAVINGS";
    private static final String CURRENCY   = "USD";

    private final LedgerTransferAccountRepository     accountRepository;
    private final LedgerTransferTransactionRepository txRepository;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransferResult transfer(TransferRequest request, Long userId) {
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "출금 계좌와 입금 계좌가 동일합니다.");
        }
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이체 금액은 0보다 커야 합니다.");
        }

        // 데드락 방지: 항상 ID 오름차순으로 락 획득
        List<Long> lockOrder = List.of(request.fromAccountId(), request.toAccountId())
                .stream().sorted().toList();

        FinancialAccount first  = lockAccount(lockOrder.get(0), userId);
        FinancialAccount second = lockAccount(lockOrder.get(1), userId);

        FinancialAccount from = first.getId().equals(request.fromAccountId()) ? first : second;
        FinancialAccount to   = first.getId().equals(request.toAccountId())   ? first : second;

        validateTransfer(from, to);

        if (from.getBalance().compareTo(request.amount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE,
                    "잔액 부족 (보유: %s USD, 요청: %s)".formatted(from.getBalance(), request.amount()));
        }

        String transferType = from.getAccountType() + "_TO_" + to.getAccountType();
        TransferTransaction tx = TransferTransaction.create(
                userId, from.getId(), to.getId(), transferType, request.amount()
        );
        tx = txRepository.save(tx);

        from.deductBalance(request.amount());
        to.addBalance(request.amount());
        accountRepository.save(from);
        accountRepository.save(to);

        tx.complete();
        tx = txRepository.save(tx);

        log.info("이체 완료: userId={} {} → {} {}USD", userId, from.getAccountType(), to.getAccountType(), request.amount());

        return new TransferResult(
                tx.getId(),
                from.getId(), from.getAccountType(),
                to.getId(),   to.getAccountType(),
                to.getVirtualAccountNumber(),
                request.amount(), CURRENCY,
                tx.getTransferStatus(),
                tx.getCompletedAt()
        );
    }

    private FinancialAccount lockAccount(Long accountId, Long userId) {
        return accountRepository.findByIdAndUserIdWithLock(accountId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "계좌를 찾을 수 없습니다. (id=" + accountId + ")"));
    }

    private void validateTransfer(FinancialAccount from, FinancialAccount to) {
        if (SAVINGS.equals(from.getAccountType())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "적금 계좌는 송금할 수 없습니다.");
        }
        if (!CURRENCY.equals(from.getCurrency()) || !CURRENCY.equals(to.getCurrency())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "USD 계좌 간 이체만 가능합니다.");
        }
    }
}