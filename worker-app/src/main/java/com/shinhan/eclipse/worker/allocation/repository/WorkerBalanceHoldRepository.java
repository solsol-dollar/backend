package com.shinhan.eclipse.worker.allocation.repository;

import com.shinhan.eclipse.domain.account.BalanceHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerBalanceHoldRepository extends JpaRepository<BalanceHold, Long> {
    Optional<BalanceHold> findBySubscriptionId(Long subscriptionId);
}
