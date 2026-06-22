package com.shinhan.eclipse.ledger.transfer.internal;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface LedgerTransferAccountRepository extends JpaRepository<FinancialAccount, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM FinancialAccount a WHERE a.id = :id AND a.userId = :userId AND a.linked = true")
    Optional<FinancialAccount> findByIdAndUserIdWithLock(
            @Param("id") Long id,
            @Param("userId") Long userId
    );
}