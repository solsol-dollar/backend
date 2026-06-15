package com.shinhan.eclipse.ledger.trade.internal;

import com.shinhan.eclipse.domain.trade.TradeOrder;
import com.shinhan.eclipse.ledger.trade.TradeOrderFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
class TradeOrderFacadeImpl implements TradeOrderFacade {

    private final TradeOrderRepository tradeOrderRepository;

    @Override
    public TradeOrder placeOrder(Long userId, Long productId, Long accountId,
                                 String orderSide, Integer quantity, BigDecimal requestedPrice) {
        // MOCK: LS증권 API 호출 없이 즉시 체결 처리
        TradeOrder order = TradeOrder.mockFill(userId, productId, accountId, orderSide, quantity, requestedPrice);
        return tradeOrderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeOrder> getOrders(Long userId) {
        return tradeOrderRepository.findByUserIdOrderByOrderedAtDesc(userId);
    }
}
