package com.shinhan.eclipse.service.app.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.auth.config.JwtConfig;
import com.shinhan.eclipse.auth.config.SecurityConfig;
import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.service.securities.TradeOrderRequest;
import com.shinhan.eclipse.service.securities.TradeOrderResponse;
import com.shinhan.eclipse.service.securities.TradeOrderService;
import com.shinhan.eclipse.service.securities.OrderHistoryItem;
import com.shinhan.eclipse.service.securities.SellProfitItem;
import com.shinhan.eclipse.service.securities.SellProfitsSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = TradeOrderController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                SpringDataWebAutoConfiguration.class
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtConfig.class)
        }
)
@Import(TradeOrderControllerTest.TestConfig.class)
@MockBean(JpaMetamodelMappingContext.class)
class TradeOrderControllerTest {

    @TestConfiguration
    static class TestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean TradeOrderService tradeOrderService;

    @BeforeEach
    void setUp() {
        AuthUser user = new AuthUser(1L, "김하늘", "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── ORD-001 ──────────────────────────────────────────────────────────────

    @Test
    void 매수주문_201_반환() throws Exception {
        TradeOrderRequest req = new TradeOrderRequest(10L, 99L, "BUY", 5, new BigDecimal("200.00"));
        TradeOrderResponse resp = new TradeOrderResponse(
                100L, "COMPLETED", "TSLA", "Tesla Inc", "BUY", 5,
                new BigDecimal("200.00"), new BigDecimal("1000.00"), "USD"
        );
        given(tradeOrderService.placeOrder(eq(1L), any(TradeOrderRequest.class))).willReturn(resp);

        mockMvc.perform(post("/api/v1/trade-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.orderId").value(100))
                .andExpect(jsonPath("$.data.orderSide").value("BUY"))
                .andExpect(jsonPath("$.data.executedAmount").value(1000.00));
    }

    @Test
    void 매도주문_201_반환() throws Exception {
        TradeOrderRequest req = new TradeOrderRequest(10L, 99L, "SELL", 3, new BigDecimal("250.00"));
        TradeOrderResponse resp = new TradeOrderResponse(
                101L, "COMPLETED", "TSLA", "Tesla Inc", "SELL", 3,
                new BigDecimal("250.00"), new BigDecimal("750.00"), "USD"
        );
        given(tradeOrderService.placeOrder(eq(1L), any(TradeOrderRequest.class))).willReturn(resp);

        mockMvc.perform(post("/api/v1/trade-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderSide").value("SELL"))
                .andExpect(jsonPath("$.data.executedAmount").value(750.00));
    }

    @Test
    void 종목없음_404_반환() throws Exception {
        TradeOrderRequest req = new TradeOrderRequest(999L, 99L, "BUY", 1, BigDecimal.TEN);
        given(tradeOrderService.placeOrder(any(), any()))
                .willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "종목을 찾을 수 없습니다: 999"));

        mockMvc.perform(post("/api/v1/trade-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("S004"))
                .andExpect(jsonPath("$.message").value("종목을 찾을 수 없습니다: 999"));
    }

    @Test
    void 잔액부족_422_반환() throws Exception {
        TradeOrderRequest req = new TradeOrderRequest(10L, 99L, "BUY", 100, new BigDecimal("500.00"));
        given(tradeOrderService.placeOrder(any(), any()))
                .willThrow(new BusinessException(ErrorCode.INSUFFICIENT_BALANCE));

        mockMvc.perform(post("/api/v1/trade-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("L001"));
    }

    @Test
    void 유효하지_않은_주문방향_400_반환() throws Exception {
        TradeOrderRequest req = new TradeOrderRequest(10L, 99L, "HOLD", 1, BigDecimal.TEN);
        given(tradeOrderService.placeOrder(any(), any()))
                .willThrow(new BusinessException(ErrorCode.INVALID_ORDER, "유효하지 않은 주문 방향: HOLD"));

        mockMvc.perform(post("/api/v1/trade-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("S006"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 주문 방향: HOLD"));
    }

    @Test
    void JWT_인증으로_주문_처리() throws Exception {
        TradeOrderRequest req = new TradeOrderRequest(10L, 99L, "BUY", 1, BigDecimal.TEN);
        TradeOrderResponse resp = new TradeOrderResponse(
                102L, "COMPLETED", "TSLA", "Tesla Inc", "BUY", 1,
                BigDecimal.TEN, BigDecimal.TEN, "USD"
        );
        given(tradeOrderService.placeOrder(eq(1L), any())).willReturn(resp);

        mockMvc.perform(post("/api/v1/trade-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderId").value(102));
    }

    // ── B-02: GET /api/v1/trade-orders ───────────────────────────────────────

    @Test
    void 주문내역_목록_조회_200() throws Exception {
        OrderHistoryItem item = new OrderHistoryItem(
                1L,
                java.time.LocalDateTime.of(2026, 6, 20, 10, 30, 0),
                "TSLA", "Tesla Inc", "BUY", "COMPLETED",
                new BigDecimal("250.00"), 5
        );
        given(tradeOrderService.getOrders(1L)).willReturn(List.of(item));

        mockMvc.perform(get("/api/v1/trade-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].ticker").value("TSLA"))
                .andExpect(jsonPath("$.data[0].orderSide").value("BUY"))
                .andExpect(jsonPath("$.data[0].quantity").value(5));
    }

    // ── B-03: GET /api/v1/trade-orders/profits ───────────────────────────────

    @Test
    void 판매수익_조회_200() throws Exception {
        SellProfitItem profitItem = new SellProfitItem(
                2L,
                java.time.LocalDateTime.of(2026, 6, 20, 15, 0, 0),
                "OVERSEAS", "AAPL", "Apple Inc",
                new BigDecimal("1050.00"), new BigDecimal("5.00"), true
        );
        SellProfitsSummary summary = new SellProfitsSummary(
                new BigDecimal("70000"), true, List.of(profitItem)
        );
        given(tradeOrderService.getProfits(1L)).willReturn(summary);

        mockMvc.perform(get("/api/v1/trade-orders/profits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalProfitKrw").value(70000))
                .andExpect(jsonPath("$.data.isProfit").value(true))
                .andExpect(jsonPath("$.data.items[0].ticker").value("AAPL"));
    }
}
