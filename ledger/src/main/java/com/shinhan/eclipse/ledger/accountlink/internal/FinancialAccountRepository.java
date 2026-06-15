package com.shinhan.eclipse.ledger.accountlink.internal;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface FinancialAccountRepository extends JpaRepository<FinancialAccount, Long> {
    List<FinancialAccount> findByUserId(Long userId);
}
