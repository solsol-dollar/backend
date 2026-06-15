package com.shinhan.eclipse.ledger.trade.internal;

import com.shinhan.eclipse.domain.trade.TradeOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {
    List<TradeOrder> findByUserIdOrderByOrderedAtDesc(Long userId);
}
