package com.shinhan.eclipse.worker.idledollar.repository;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IdleDetectionAccountRepository extends JpaRepository<FinancialAccount, Long> {

    @Query("""
            SELECT a FROM FinancialAccount a
            WHERE a.accountType = 'SECURITIES'
              AND a.currency = 'USD'
              AND a.linked = true
              AND a.balance > 0
              AND a.status = 'ACTIVE'
            """)
    List<FinancialAccount> findActiveSecuritiesAccountsWithBalance();
}
