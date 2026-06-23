package com.shinhan.eclipse.ledger.accountlink.internal;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface FinancialAccountRepository extends JpaRepository<FinancialAccount, Long> {
    List<FinancialAccount> findByUserId(Long userId);

    Optional<FinancialAccount> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from FinancialAccount a where a.id = :id and a.userId = :userId")
    Optional<FinancialAccount> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    Optional<FinancialAccount> findFirstByUserIdAndAccountTypeAndLinkedTrueOrderByIdAsc(Long userId, String accountType);

    Optional<FinancialAccount> findFirstByUserIdAndAccountTypeOrderByIdAsc(Long userId, String accountType);
}
