package com.shinhan.eclipse.service.mypage.internal;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface MyPageAccountRepository extends JpaRepository<FinancialAccount, Long> {
    List<FinancialAccount> findByUserIdAndLinkedTrueAndStatus(Long userId, String status);
    boolean existsByUserIdAndAccountTypeAndStatus(Long userId, String accountType, String status);
    Optional<FinancialAccount> findFirstByUserIdAndAccountTypeAndStatus(Long userId, String accountType, String status);
    Optional<FinancialAccount> findFirstByUserIdAndAccountTypeAndCurrencyAndLinkedTrueAndStatus(Long userId, String accountType, String currency, String status);
    Optional<FinancialAccount> findByUserIdAndAccountTypeAndCurrencyAndStatus(Long userId, String accountType, String currency, String status);
}