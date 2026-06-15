package com.shinhan.eclipse.ledger.subscription.internal;

import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface IpoSubscriptionRepository extends JpaRepository<IpoSubscription, Long> {
    List<IpoSubscription> findByUserId(Long userId);
}
