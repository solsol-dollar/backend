package com.shinhan.eclipse.ledger.subscription.internal;

import com.shinhan.eclipse.domain.account.BalanceHold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface BalanceHoldRepository extends JpaRepository<BalanceHold, Long> {
    Optional<BalanceHold> findBySubscriptionId(Long subscriptionId);
}
