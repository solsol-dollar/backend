package com.shinhan.eclipse.ledger.accountlink;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountLinkService {
    List<FinancialAccount> getLinkedAccounts(Long userId);

    /** 사용자의 특정 유형(연동) 계좌 1건 조회. 여러 건이면 첫 번째. */
    Optional<FinancialAccount> findAccountByType(Long userId, String accountType);
    FinancialAccount linkAccount(Long userId, String accountType, String institutionName, String accountNumberMasked);
    void unlinkAccount(Long userId, Long accountId);

    /** 연동 계좌 단건 조회. 본인 소유가 아니거나 연동되지 않은 계좌면 L005(ACCOUNT_NOT_LINKED). */
    FinancialAccount getLinkedAccount(Long userId, Long accountId);

    /** 청약 확정 등 잔액 차감 전 비관적 락으로 계좌를 잠금 조회. */
    FinancialAccount lockAccount(Long userId, Long accountId);

    /** 소유/연동 검증 + 비관적 락 후 차감. */
    void deduct(Long userId, Long accountId, BigDecimal amount);

    /** 소유/연동 검증 + 비관적 락 후 적립. 리턴 플랜 분배 실행 등에서 사용. */
    void credit(Long userId, Long accountId, BigDecimal amount);
}
