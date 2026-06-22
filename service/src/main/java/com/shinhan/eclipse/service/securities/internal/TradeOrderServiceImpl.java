package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.holding.Holding;
import com.shinhan.eclipse.domain.holding.HoldingLot;
import com.shinhan.eclipse.domain.product.InvestmentProduct;
import com.shinhan.eclipse.domain.trade.TradeOrder;
import com.shinhan.eclipse.service.securities.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
class TradeOrderServiceImpl implements TradeOrderService {

    private final ProductRepository    productRepository;
    private final HoldingRepository    holdingRepository;
    private final HoldingLotRepository holdingLotRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final QuoteCache           quoteCache;
    private final LedgerClient         ledgerClient;

    @Value("${eclipse.fx.usd-krw:1368.5}")
    private BigDecimal usdKrw;

    @Override
    @Transactional
    public TradeOrderResponse placeOrder(Long userId, TradeOrderRequest req) {
        InvestmentProduct product = productRepository.findById(req.productId())
                .filter(p -> "ACTIVE".equals(p.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND,
                        "종목을 찾을 수 없습니다: " + req.productId()));

        BigDecimal executedPrice = resolvePrice(req, product);
        BigDecimal executedAmount = executedPrice.multiply(BigDecimal.valueOf(req.quantity()));

        return switch (req.orderSide()) {
            case "BUY"  -> executeBuy(userId, product, req, executedPrice, executedAmount);
            case "SELL" -> executeSell(userId, product, req, executedPrice, executedAmount);
            default     -> throw new BusinessException(ErrorCode.INVALID_ORDER,
                    "유효하지 않은 주문 방향: " + req.orderSide());
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderHistoryItem> getOrders(Long userId) {
        List<TradeOrder> orders = tradeOrderRepository.findByUserIdOrderByOrderedAtDesc(userId);
        List<OrderHistoryItem> result = new ArrayList<>();

        for (TradeOrder order : orders) {
            Optional<InvestmentProduct> productOpt = productRepository.findById(order.getProductId());
            String ticker = productOpt.map(InvestmentProduct::getTicker).orElse("UNKNOWN");
            String productName = productOpt.map(InvestmentProduct::getProductName).orElse("삭제된 종목");

            result.add(new OrderHistoryItem(
                    order.getId(),
                    order.getOrderedAt(),
                    ticker,
                    productName,
                    order.getOrderSide(),
                    order.getOrderStatus(),
                    order.getExecutedPrice(),
                    order.getQuantity()
            ));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public SellProfitsSummary getProfits(Long userId) {
        List<TradeOrder> sellOrders = tradeOrderRepository
                .findByUserIdAndOrderSideOrderByOrderedAtDesc(userId, "SELL");

        List<SellProfitItem> items = new ArrayList<>();
        BigDecimal totalProfitUsd = BigDecimal.ZERO;

        for (TradeOrder order : sellOrders) {
            Optional<InvestmentProduct> productOpt = productRepository.findById(order.getProductId());
            if (productOpt.isEmpty()) continue;

            InvestmentProduct product = productOpt.get();
            Optional<Holding> holdingOpt = holdingRepository.findByUserIdAndProductId(userId, product.getId());

            BigDecimal avgPrice = holdingOpt
                    .map(Holding::getAveragePrice)
                    .orElse(order.getExecutedPrice());

            BigDecimal profitUsd = order.getExecutedPrice()
                    .subtract(avgPrice)
                    .multiply(BigDecimal.valueOf(order.getQuantity()));

            BigDecimal profitRate = avgPrice.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : order.getExecutedPrice().subtract(avgPrice)
                            .divide(avgPrice, 6, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));

            totalProfitUsd = totalProfitUsd.add(profitUsd);

            items.add(new SellProfitItem(
                    order.getId(),
                    order.getOrderedAt(),
                    product.getProductType(),
                    product.getTicker(),
                    product.getProductName(),
                    order.getExecutedAmount(),
                    profitRate,
                    profitUsd.compareTo(BigDecimal.ZERO) >= 0
            ));
        }

        BigDecimal totalProfitKrw = totalProfitUsd.multiply(usdKrw).setScale(0, RoundingMode.HALF_UP);
        return new SellProfitsSummary(totalProfitKrw, totalProfitKrw.compareTo(BigDecimal.ZERO) >= 0, items);
    }

    private BigDecimal resolvePrice(TradeOrderRequest req, InvestmentProduct product) {
        if (req.requestedPrice() != null && req.requestedPrice().compareTo(BigDecimal.ZERO) > 0) {
            return req.requestedPrice();
        }
        return quoteCache.get(product.getTicker())
                .map(QuoteSnapshot::price)
                .filter(p -> p.compareTo(BigDecimal.ZERO) > 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUOTE_UNAVAILABLE));
    }

    private TradeOrderResponse executeBuy(Long userId, InvestmentProduct product,
                                          TradeOrderRequest req,
                                          BigDecimal executedPrice, BigDecimal executedAmount) {
        Saga saga = new Saga("BuyTrade[userId=" + userId + ",ticker=" + product.getTicker() + "]");
        try {
            // Step 1: 잔고 차감 (ledger-app) — 보상: 잔고 복구
            saga.step("잔고 차감",
                    () -> ledgerClient.deductBalance(userId, req.accountId(), executedAmount),
                    () -> ledgerClient.addBalance(userId, req.accountId(), executedAmount)
            );

            // Step 2~4: service DB — 실패 시 @Transactional 롤백이 처리, Saga 보상은 Step 1용
            TradeOrder order = saga.step("주문 저장",
                    () -> tradeOrderRepository.save(TradeOrder.mockFill(
                            userId, product.getId(), req.accountId(),
                            "BUY", req.quantity(), executedPrice)),
                    () -> {} // DB 롤백은 @Transactional이 담당
            );

            Holding holding = holdingRepository
                    .findByUserIdAndProductId(userId, product.getId())
                    .map(h -> { h.addBuy(req.quantity(), executedPrice); return h; })
                    .orElseGet(() -> Holding.create(userId, product.getId(), req.quantity(), executedPrice));

            Holding savedHolding = saga.step("보유 업데이트",
                    () -> holdingRepository.save(holding),
                    () -> {}
            );

            saga.step("보유 로트 저장",
                    () -> holdingLotRepository.save(
                            HoldingLot.ofBuy(savedHolding.getId(), userId, product.getId(),
                                    order.getId(), req.quantity(), executedPrice)),
                    () -> {}
            );

            log.info("매수 체결: userId={} ticker={} qty={} price={}",
                    userId, product.getTicker(), req.quantity(), executedPrice);
            return TradeOrderResponse.of(order, product.getTicker(), product.getProductName());

        } catch (Exception e) {
            saga.compensate(e);
            if (e instanceof BusinessException be) throw be;
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private TradeOrderResponse executeSell(Long userId, InvestmentProduct product,
                                           TradeOrderRequest req,
                                           BigDecimal executedPrice, BigDecimal executedAmount) {
        Holding holding = holdingRepository
                .findByUserIdAndProductId(userId, product.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INSUFFICIENT_HOLDING,
                        "해당 종목을 보유하고 있지 않습니다: " + product.getTicker()));

        if (holding.getTotalQuantity() < req.quantity()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_HOLDING,
                    "보유 수량 부족 (보유: %d, 요청: %d)".formatted(holding.getTotalQuantity(), req.quantity()));
        }

        Saga saga = new Saga("SellTrade[userId=" + userId + ",ticker=" + product.getTicker() + "]");
        try {
            // Step 1~2: service DB — 실패 시 @Transactional 롤백
            holding.addSell(req.quantity());
            saga.step("보유 차감",
                    () -> holdingRepository.save(holding),
                    () -> {}
            );

            TradeOrder order = saga.step("주문 저장",
                    () -> tradeOrderRepository.save(TradeOrder.mockFill(
                            userId, product.getId(), req.accountId(),
                            "SELL", req.quantity(), executedPrice)),
                    () -> {}
            );

            // Step 3: 잔고 증가 (ledger-app) — 실패 시 @Transactional이 DB 롤백하므로
            //          보상으로 잔고 다시 차감 불필요. 단, 멱등성을 위해 명시적으로 정의.
            saga.step("잔고 증가",
                    () -> ledgerClient.addBalance(userId, req.accountId(), executedAmount),
                    () -> ledgerClient.deductBalance(userId, req.accountId(), executedAmount)
            );

            log.info("매도 체결: userId={} ticker={} qty={} price={}",
                    userId, product.getTicker(), req.quantity(), executedPrice);
            return TradeOrderResponse.of(order, product.getTicker(), product.getProductName());

        } catch (Exception e) {
            saga.compensate(e);
            if (e instanceof BusinessException be) throw be;
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }
}
