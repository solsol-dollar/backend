package com.shinhan.eclipse.service.app.auth.internal;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingAccountRepository extends JpaRepository<FinancialAccount, Long> {
    List<FinancialAccount> findByUserIdAndAccountTypeInAndLinkedFalse(Long userId, List<String> accountTypes);
}