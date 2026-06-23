package com.shinhan.eclipse.service.home.internal;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface AssetAccountRepository extends JpaRepository<FinancialAccount, Long> {
    List<FinancialAccount> findByUserIdAndLinkedTrueAndStatus(Long userId, String status);
}