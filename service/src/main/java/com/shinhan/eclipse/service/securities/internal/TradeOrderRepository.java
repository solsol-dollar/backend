package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.domain.trade.TradeOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {
    List<TradeOrder> findByUserIdOrderByOrderedAtDesc(Long userId);
    List<TradeOrder> findByUserIdAndOrderSideOrderByOrderedAtDesc(Long userId, String orderSide);
}
