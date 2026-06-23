package com.shinhan.eclipse.ledger.transaction.internal;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface TxAccountRepository extends JpaRepository<FinancialAccount, Long> {
    List<FinancialAccount> findAllByIdIn(List<Long> ids);
}