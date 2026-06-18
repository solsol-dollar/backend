package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.domain.trade.TradeOrder;
import org.springframework.data.jpa.repository.JpaRepository;

interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {
}
