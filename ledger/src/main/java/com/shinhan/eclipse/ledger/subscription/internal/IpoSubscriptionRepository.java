package com.shinhan.eclipse.ledger.subscription.internal;

import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

interface IpoSubscriptionRepository extends JpaRepository<IpoSubscription, Long> {
    List<IpoSubscription> findByUserId(Long userId);

    Optional<IpoSubscription> findByIdAndUserId(Long id, Long userId);

    /** 동일 청약 동시 확정(REQ-05-93-01) 방지를 위한 락 조회. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from IpoSubscription s where s.id = :id and s.userId = :userId")
    Optional<IpoSubscription> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    /** 조회 조건(ipoId/상태/기간) 필터 — 전부 선택값이며 null이면 해당 조건은 무시된다 (명세 외 추가). */
    @Query("""
            select s from IpoSubscription s
            where s.userId = :userId
              and (:ipoId is null or s.ipoId = :ipoId)
              and (:status is null or upper(s.subscriptionStatus) = upper(:status))
              and (:fromDateTime is null or s.subscribedAt >= :fromDateTime)
              and (:toDateTime is null or s.subscribedAt <= :toDateTime)
            order by s.subscribedAt desc
            """)
    List<IpoSubscription> search(@Param("userId") Long userId,
                                  @Param("ipoId") Long ipoId,
                                  @Param("status") String status,
                                  @Param("fromDateTime") LocalDateTime fromDateTime,
                                  @Param("toDateTime") LocalDateTime toDateTime);
}
