package com.shinhan.eclipse.service.inflow.internal;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface IdleDetectionAccountRepository extends JpaRepository<FinancialAccount, Long> {

    @Query("""
            SELECT a FROM FinancialAccount a
            WHERE a.accountType = 'SECURITIES'
              AND a.linked = true
              AND a.balance > 0
              AND a.status = 'ACTIVE'
            """)
    List<FinancialAccount> findActiveSecuritiesAccountsWithBalance();
}