package com.shinhan.eclipse.service.inflow.internal;

import com.shinhan.eclipse.domain.trade.TradeOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

interface IdleDetectionTradeOrderRepository extends JpaRepository<TradeOrder, Long> {

    @Query("SELECT MAX(t.orderedAt) FROM TradeOrder t WHERE t.accountId = :accountId AND t.status = 'ACTIVE'")
    Optional<LocalDateTime> findLatestOrderedAt(@Param("accountId") Long accountId);
}