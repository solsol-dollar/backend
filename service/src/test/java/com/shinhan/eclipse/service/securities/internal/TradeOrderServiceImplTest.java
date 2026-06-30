package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.holding.Holding;
import com.shinhan.eclipse.domain.product.InvestmentProduct;
import com.shinhan.eclipse.domain.trade.TradeOrder;
import com.shinhan.eclipse.service.securities.QuoteCache;
import com.shinhan.eclipse.service.securities.QuoteSnapshot;
import com.shinhan.eclipse.service.securities.TradeOrderRequest;
import com.shinhan.eclipse.service.securities.TradeOrderResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TradeOrderServiceImplTest {

    @Mock ProductRepository         productRepository;
    @Mock HoldingRepository         holdingRepository;
    @Mock HoldingLotRepository      holdingLotRepository;
    @Mock TradeOrderRepository      tradeOrderRepository;
    @Mock FinancialAccountRepository accountRepository;
    @Mock QuoteCache                quoteCache;

    @InjectMocks TradeOrderServiceImpl service;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private InvestmentProduct activeProduct(Long id, String ticker) {
        return InvestmentProduct.ofSeed("OVERSEAS", ticker, ticker + " Inc",
                "NASDAQ", "USD", "Technology");
    }

    private TradeOrder savedOrder(Long id, String orderSide, Integer qty, BigDecimal price) {
        TradeOrder order = TradeOrder.mockFill(1L, 10L, 99L, orderSide, qty, price);
        try {
            var f = com.shinhan.eclipse.common.entity.BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(order, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return order;
    }

    // ── 매수 ─────────────────────────────────────────────────────────────────

    @Test
    void 매수_정상_체결() {
        InvestmentProduct product = activeProduct(10L, "TSLA");
        TradeOrder order = savedOrder(100L, "BUY", 5, new BigDecimal("200.00"));
        FinancialAccount account = mock(FinancialAccount.class);

        given(productRepository.findById(10L)).willReturn(Optional.of(product));
        given(accountRepository.findByIdAndUserIdForUpdate(99L, 1L)).willReturn(Optional.of(account));
        given(holdingRepository.findByUserIdAndProductId(1L, null)).willReturn(Optional.empty());
        given(holdingRepository.save(any())).willAnswer(inv -> {
            Holding h = inv.getArgument(0);
            try {
                var f = com.shinhan.eclipse.common.entity.BaseEntity.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(h, 50L);
            } catch (Exception e) { throw new RuntimeException(e); }
            return h;
        });
        given(tradeOrderRepository.save(any())).willReturn(order);
        given(holdingLotRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        TradeOrderRequest req = new TradeOrderRequest(10L, 99L, "BUY", 5, new BigDecimal("200.00"));

        TradeOrderResponse response = service.placeOrder(1L, req);

        assertThat(response.orderId()).isEqualTo(100L);
        assertThat(response.orderSide()).isEqualTo("BUY");
        assertThat(response.executedPrice()).isEqualByComparingTo("200.00");
        verify(tradeOrderRepository).save(any(TradeOrder.class));
    }

    @Test
    void 매수_잔액_부족이면_예외() {
        InvestmentProduct product = activeProduct(10L, "TSLA");
        FinancialAccount account = mock(FinancialAccount.class);

        given(productRepository.findById(10L)).willReturn(Optional.of(product));
        given(accountRepository.findByIdAndUserIdForUpdate(99L, 1L)).willReturn(Optional.of(account));
        willThrow(new BusinessException(ErrorCode.INSUFFICIENT_BALANCE))
                .given(account).deductBalance(any());

        TradeOrderRequest req = new TradeOrderRequest(10L, 99L, "BUY", 5, new BigDecimal("200.00"));

        assertThatThrownBy(() -> service.placeOrder(1L, req))
                .isInstanceOf(BusinessException.class);
    }

    // ── 매도 ─────────────────────────────────────────────────────────────────

    @Test
    void 매도_정상_체결() {
        InvestmentProduct product = activeProduct(10L, "TSLA");
        Holding holding = Holding.create(1L, null, 10, new BigDecimal("180.00"));
        TradeOrder order = savedOrder(101L, "SELL", 3, new BigDecimal("200.00"));
        FinancialAccount account = mock(FinancialAccount.class);

        given(productRepository.findById(10L)).willReturn(Optional.of(product));
        given(holdingRepository.findByUserIdAndProductId(1L, null)).willReturn(Optional.of(holding));
        given(holdingRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(tradeOrderRepository.save(any())).willReturn(order);
        given(accountRepository.findByIdAndUserIdForUpdate(99L, 1L)).willReturn(Optional.of(account));

        TradeOrderRequest req = new TradeOrderRequest(10L, 99L, "SELL", 3, new BigDecimal("200.00"));

        TradeOrderResponse response = service.placeOrder(1L, req);

        assertThat(response.orderId()).isEqualTo(101L);
        assertThat(response.orderSide()).isEqualTo("SELL");
        assertThat(holding.getTotalQuantity()).isEqualTo(7); // 10 - 3
    }

    @Test
    void 매도_보유수량_초과시_예외() {
        InvestmentProduct product = activeProduct(10L, "TSLA");
        Holding holding = Holding.create(1L, null, 2, new BigDecimal("180.00")); // 2주만 보유

        given(productRepository.findById(10L)).willReturn(Optional.of(product));
        given(holdingRepository.findByUserIdAndProductId(1L, null)).willReturn(Optional.of(holding));

        TradeOrderRequest req = new TradeOrderRequest(10L, 99L, "SELL", 5, new BigDecimal("200.00"));

        assertThatThrownBy(() -> service.placeOrder(1L, req))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 종목이_없으면_예외() {
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        TradeOrderRequest req = new TradeOrderRequest(999L, 99L, "BUY", 1, BigDecimal.TEN);

        assertThatThrownBy(() -> service.placeOrder(1L, req))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 가격_미입력_시_Redis_캐시에서_가져온다() {
        InvestmentProduct product = activeProduct(10L, "TSLA");
        QuoteSnapshot snapshot = new QuoteSnapshot("TSLA", new BigDecimal("250.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, 0L, "3", Instant.now());
        TradeOrder order = savedOrder(102L, "BUY", 1, new BigDecimal("250.00"));

        FinancialAccount account = mock(FinancialAccount.class);
        given(productRepository.findById(10L)).willReturn(Optional.of(product));
        given(accountRepository.findByIdAndUserIdForUpdate(99L, 1L)).willReturn(Optional.of(account));
        given(quoteCache.get("TSLA")).willReturn(Optional.of(snapshot));
        given(holdingRepository.findByUserIdAndProductId(any(), any())).willReturn(Optional.empty());
        given(holdingRepository.save(any())).willAnswer(inv -> {
            Holding h = inv.getArgument(0);
            try {
                var f = com.shinhan.eclipse.common.entity.BaseEntity.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(h, 50L);
            } catch (Exception e) { throw new RuntimeException(e); }
            return h;
        });
        given(tradeOrderRepository.save(any())).willReturn(order);
        given(holdingLotRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        TradeOrderRequest req = new TradeOrderRequest(10L, 99L, "BUY", 1, null);

        TradeOrderResponse response = service.placeOrder(1L, req);

        assertThat(response.executedPrice()).isEqualByComparingTo("250.00");
    }
}
