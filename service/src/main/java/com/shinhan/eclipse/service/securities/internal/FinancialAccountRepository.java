package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface FinancialAccountRepository extends JpaRepository<FinancialAccount, Long> {
    Optional<FinancialAccount> findByIdAndUserId(Long id, Long userId);
    List<FinancialAccount> findByUserId(Long userId);
}
