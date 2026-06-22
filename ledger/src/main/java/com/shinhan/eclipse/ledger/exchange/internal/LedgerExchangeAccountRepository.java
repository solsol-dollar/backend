package com.shinhan.eclipse.ledger.exchange.internal;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface LedgerExchangeAccountRepository extends JpaRepository<FinancialAccount, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM FinancialAccount a WHERE a.userId = :userId AND a.currency = :currency AND a.accountType = 'SECURITIES' AND a.linked = true")
    Optional<FinancialAccount> findLinkedAccountWithLock(
            @Param("userId") Long userId,
            @Param("currency") String currency
    );
}