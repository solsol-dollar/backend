package com.shinhan.eclipse.ledger.returnplan.internal;

import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface ReturnPlanRepository extends JpaRepository<ReturnPlan, Long> {
    List<ReturnPlan> findByUserId(Long userId);

    Page<ReturnPlan> findByUserId(Long userId, Pageable pageable);

    Optional<ReturnPlan> findByIdAndUserId(Long id, Long userId);

    boolean existsBySubscriptionId(Long subscriptionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ReturnPlan p where p.id = :id and p.userId = :userId")
    Optional<ReturnPlan> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}
