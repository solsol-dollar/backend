package com.shinhan.eclipse.ledger.subscription.internal;

import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface IpoSubscriptionRepository extends JpaRepository<IpoSubscription, Long> {
    List<IpoSubscription> findByUserId(Long userId);

    Optional<IpoSubscription> findByIdAndUserId(Long id, Long userId);

    /** 동일 청약 동시 확정(REQ-05-93-01) 방지를 위한 락 조회. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from IpoSubscription s where s.id = :id and s.userId = :userId")
    Optional<IpoSubscription> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    List<IpoSubscription> findByUserIdAndIpoId(Long userId, Long ipoId);

    List<IpoSubscription> findByUserIdAndSubscriptionStatus(Long userId, String subscriptionStatus);

    List<IpoSubscription> findByUserIdAndIpoIdAndSubscriptionStatus(Long userId, Long ipoId, String subscriptionStatus);
}
