package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.holding.Holding;
import com.shinhan.eclipse.domain.holding.HoldingLot;
import com.shinhan.eclipse.domain.product.InvestmentProduct;
import com.shinhan.eclipse.domain.trade.TradeOrder;
import com.shinhan.eclipse.service.securities.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
class TradeOrderServiceImpl implements TradeOrderService {

    private final ProductRepository          productRepository;
    private final FinancialAccountRepository accountRepository;
    private final HoldingRepository          holdingRepository;
    private final HoldingLotRepository       holdingLotRepository;
    private final TradeOrderRepository       tradeOrderRepository;
    private final QuoteCache                 quoteCache;

    @Override
    @Transactional
    public TradeOrderResponse placeOrder(Long userId, TradeOrderRequest req) {
        InvestmentProduct product = productRepository.findById(req.productId())
                .filter(p -> "ACTIVE".equals(p.getStatus()))
                .orElseThrow(() -> new NoSuchElementException("종목을 찾을 수 없습니다: " + req.productId()));

        FinancialAccount account = accountRepository.findByIdAndUserId(req.accountId(), userId)
                .orElseThrow(() -> new NoSuchElementException("계좌를 찾을 수 없습니다: " + req.accountId()));

        BigDecimal executedPrice = resolvePrice(req, product);
        BigDecimal executedAmount = executedPrice.multiply(BigDecimal.valueOf(req.quantity()));

        return switch (req.orderSide()) {
            case "BUY"  -> executeBuy(userId, product, account, req, executedPrice, executedAmount);
            case "SELL" -> executeSell(userId, product, account, req, executedPrice, executedAmount);
            default     -> throw new IllegalArgumentException("유효하지 않은 주문 방향: " + req.orderSide());
        };
    }

    private BigDecimal resolvePrice(TradeOrderRequest req, InvestmentProduct product) {
        if (req.requestedPrice() != null && req.requestedPrice().compareTo(BigDecimal.ZERO) > 0) {
            return req.requestedPrice();
        }
        return quoteCache.get(product.getTicker())
                .map(QuoteSnapshot::price)
                .filter(p -> p.compareTo(BigDecimal.ZERO) > 0)
                .orElseThrow(() -> new IllegalStateException("현재가를 조회할 수 없습니다 (장 외 시간). 가격을 직접 입력해 주세요."));
    }

    private TradeOrderResponse executeBuy(Long userId, InvestmentProduct product,
                                          FinancialAccount account, TradeOrderRequest req,
                                          BigDecimal executedPrice, BigDecimal executedAmount) {
        account.deductBalance(executedAmount);
        accountRepository.save(account);

        TradeOrder order = TradeOrder.mockFill(
                userId, product.getId(), account.getId(),
                "BUY", req.quantity(), executedPrice
        );
        order = tradeOrderRepository.save(order);

        Holding holding = holdingRepository
                .findByUserIdAndProductId(userId, product.getId())
                .map(h -> { h.addBuy(req.quantity(), executedPrice); return h; })
                .orElseGet(() -> Holding.create(userId, product.getId(), req.quantity(), executedPrice));
        holding = holdingRepository.save(holding);

        holdingLotRepository.save(
                HoldingLot.ofBuy(holding.getId(), userId, product.getId(),
                        order.getId(), req.quantity(), executedPrice)
        );

        log.info("매수 체결: userId={} ticker={} qty={} price={}", userId, product.getTicker(), req.quantity(), executedPrice);
        return TradeOrderResponse.of(order, product.getTicker());
    }

    private TradeOrderResponse executeSell(Long userId, InvestmentProduct product,
                                           FinancialAccount account, TradeOrderRequest req,
                                           BigDecimal executedPrice, BigDecimal executedAmount) {
        Holding holding = holdingRepository
                .findByUserIdAndProductId(userId, product.getId())
                .orElseThrow(() -> new IllegalStateException("해당 종목을 보유하고 있지 않습니다: " + product.getTicker()));

        holding.addSell(req.quantity());
        holdingRepository.save(holding);

        account.addBalance(executedAmount);
        accountRepository.save(account);

        TradeOrder order = TradeOrder.mockFill(
                userId, product.getId(), account.getId(),
                "SELL", req.quantity(), executedPrice
        );
        order = tradeOrderRepository.save(order);

        log.info("매도 체결: userId={} ticker={} qty={} price={}", userId, product.getTicker(), req.quantity(), executedPrice);
        return TradeOrderResponse.of(order, product.getTicker());
    }
}
