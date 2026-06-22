package com.shinhan.eclipse.ledger.balance.internal;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface BalanceAccountRepository extends JpaRepository<FinancialAccount, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from FinancialAccount a where a.id = :id and a.userId = :userId")
    Optional<FinancialAccount> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}
