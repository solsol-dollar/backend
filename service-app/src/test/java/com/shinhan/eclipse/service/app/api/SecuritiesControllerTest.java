package com.shinhan.eclipse.service.app.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.auth.config.JwtConfig;
import com.shinhan.eclipse.auth.config.SecurityConfig;
import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.service.securities.*;
import com.shinhan.eclipse.service.securities.ProductStats;
import com.shinhan.eclipse.service.securities.RankingItem;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = SecuritiesController.class,
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
@Import(SecuritiesControllerTest.TestConfig.class)
@MockBean(JpaMetamodelMappingContext.class)
class SecuritiesControllerTest {

    @TestConfiguration
    static class TestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean SecuritiesService securitiesService;
    @MockBean ChartService chartService;

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

    // ── SEC-001 ──────────────────────────────────────────────────────────────

    @Test
    void listProducts_200_ApiResponse_래퍼로_반환() throws Exception {
        ProductListItem item = new ProductListItem(
                1L, "TSLA", "Tesla Inc", "OVERSEAS", "NASDAQ", "USD",
                "Consumer Discretionary", new BigDecimal("250.00"), new BigDecimal("1.29"), "2",
                1000000L, new BigDecimal("250000000"), List.of()
        );
        given(securitiesService.listProducts(null, null, null)).willReturn(List.of(item));

        mockMvc.perform(get("/api/v1/securities/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].ticker").value("TSLA"))
                .andExpect(jsonPath("$.data[0].price").value(250.00));
    }

    @Test
    void listProducts_타입_키워드_파라미터_전달() throws Exception {
        given(securitiesService.listProducts("OVERSEAS", "TSLA", null)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/securities/products")
                        .param("type", "OVERSEAS")
                        .param("keyword", "TSLA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        org.mockito.Mockito.verify(securitiesService).listProducts("OVERSEAS", "TSLA", null);
    }

    // ── SEC-002 ──────────────────────────────────────────────────────────────

    @Test
    void getProduct_200_반환() throws Exception {
        ProductDetail detail = new ProductDetail(
                1L, "AAPL", "Apple Inc", "OVERSEAS", "NASDAQ", "USD",
                "Technology", new BigDecimal("185.00"), new BigDecimal("2.00"),
                new BigDecimal("1.09"), "2", 5000000L, Instant.now()
        );
        given(securitiesService.getProduct(1L)).willReturn(detail);

        mockMvc.perform(get("/api/v1/securities/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("AAPL"))
                .andExpect(jsonPath("$.data.price").value(185.00));
    }

    @Test
    void getProduct_없는_id면_404() throws Exception {
        given(securitiesService.getProduct(999L))
                .willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "종목을 찾을 수 없습니다: 999"));

        mockMvc.perform(get("/api/v1/securities/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("S004"))
                .andExpect(jsonPath("$.message").value("종목을 찾을 수 없습니다: 999"));
    }

    // ── SEC-003 ──────────────────────────────────────────────────────────────

    @Test
    void getOrderBook_200_반환() throws Exception {
        OrderBookResponse resp = new OrderBookResponse(
                "NVDA", new BigDecimal("880.00"),
                List.of(new OrderBookResponse.Level(new BigDecimal("880.50"), 100L)),
                List.of(new OrderBookResponse.Level(new BigDecimal("879.50"), 200L))
        );
        given(securitiesService.getOrderBook(1L)).willReturn(resp);

        mockMvc.perform(get("/api/v1/securities/products/1/quotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("NVDA"))
                .andExpect(jsonPath("$.data.askLevels[0].price").value(880.50));
    }

    // ── SEC-004 ──────────────────────────────────────────────────────────────

    @Test
    void getHoldings_JWT_인증으로_조회() throws Exception {
        HoldingItem holding = new HoldingItem(
                1L, 10L, "TSLA", "Tesla Inc", "OVERSEAS", "NASDAQ", 5,
                new BigDecimal("200.00"), "USD", new BigDecimal("250.00"),
                new BigDecimal("1250.00"), new BigDecimal("250.00"), new BigDecimal("25.00")
        );
        HoldingsSummary summary = new HoldingsSummary(
                new BigDecimal("1250.00"), new BigDecimal("1000.00"),
                new BigDecimal("15.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(holding)
        );
        given(securitiesService.getHoldings(1L)).willReturn(summary);

        mockMvc.perform(get("/api/v1/securities/holdings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.holdings[0].ticker").value("TSLA"))
                .andExpect(jsonPath("$.data.holdings[0].profitLoss").value(250.00));
    }

    // ── SEC-005 ──────────────────────────────────────────────────────────────

    @Test
    void getRecommended_AI_추천_목록_반환() throws Exception {
        RecommendedProduct rec = new RecommendedProduct(
                1L, "NVDA", "NVIDIA Corp", "OVERSEAS", "Semiconductors", "NASDAQ",
                new BigDecimal("880.00"), new BigDecimal("2.5"), "2", "AI/데이터센터 수혜주"
        );
        given(securitiesService.getRecommended(1L)).willReturn(List.of(rec));

        mockMvc.perform(get("/api/v1/securities/recommended"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ticker").value("NVDA"))
                .andExpect(jsonPath("$.data[0].reason").value("AI/데이터센터 수혜주"));
    }

    // ── SEC-006 ──────────────────────────────────────────────────────────────

    @Test
    void getChart_1M_월봉_응답_반환() throws Exception {
        ChartResponse.CandleItem candle = new ChartResponse.CandleItem(
                "20260601", null,
                new BigDecimal("185.00"), new BigDecimal("190.00"),
                new BigDecimal("183.50"), new BigDecimal("188.00"),
                45000000L, "2"
        );
        ChartResponse chartResponse = new ChartResponse("AAPL", "1M", "MONTH", List.of(candle));
        given(chartService.getChart(1L, "1M")).willReturn(chartResponse);

        mockMvc.perform(get("/api/v1/securities/products/1/chart").param("period", "1M"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.ticker").value("AAPL"))
                .andExpect(jsonPath("$.data.period").value("1M"))
                .andExpect(jsonPath("$.data.candleType").value("MONTH"))
                .andExpect(jsonPath("$.data.candles[0].date").value("20260601"))
                .andExpect(jsonPath("$.data.candles[0].close").value(188.00));
    }

    @Test
    void getChart_5MIN_분봉_time_필드_포함() throws Exception {
        ChartResponse.CandleItem candle = new ChartResponse.CandleItem(
                "20260617", "093000",
                new BigDecimal("193.00"), new BigDecimal("194.00"),
                new BigDecimal("192.50"), new BigDecimal("193.80"),
                1234567L, null
        );
        ChartResponse chartResponse = new ChartResponse("AAPL", "5MIN", "MINUTE", List.of(candle));
        given(chartService.getChart(1L, "5MIN")).willReturn(chartResponse);

        mockMvc.perform(get("/api/v1/securities/products/1/chart")
                        .param("period", "5MIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candleType").value("MINUTE"))
                .andExpect(jsonPath("$.data.candles[0].time").value("093000"));
    }

    @Test
    void getChart_잘못된_period_400_반환() throws Exception {
        mockMvc.perform(get("/api/v1/securities/products/1/chart").param("period", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void getChart_period_기본값_1M() throws Exception {
        ChartResponse chartResponse = new ChartResponse("AAPL", "1M", "MONTH", List.of());
        given(chartService.getChart(1L, "1M")).willReturn(chartResponse);

        mockMvc.perform(get("/api/v1/securities/products/1/chart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period").value("1M"))
                .andExpect(jsonPath("$.data.candleType").value("MONTH"));
    }

    // ── B-01: 시장 인덱스 ────────────────────────────────────────────────────

    @Test
    void getMarketIndices_200_반환() throws Exception {
        MarketIndex sp500 = new MarketIndex("S&P 500", new BigDecimal("5308.13"),
                new BigDecimal("-500.77"), new BigDecimal("-1.60"), false, false);
        given(securitiesService.getMarketIndices()).willReturn(List.of(sp500));

        mockMvc.perform(get("/api/v1/securities/market/indices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].name").value("S&P 500"))
                .andExpect(jsonPath("$.data[0].isMarketOpen").value(false));
    }

    // ── SEC-007 ──────────────────────────────────────────────────────────────

    @Test
    void getProductStats_200_반환() throws Exception {
        ProductStats stats = new ProductStats(
                "AAPL",
                new BigDecimal("260.10"),
                new BigDecimal("164.08"),
                Map.of("1M", new BigDecimal("5.23"), "1Y", new BigDecimal("28.91"))
        );
        given(securitiesService.getProductStats(1L)).willReturn(stats);

        mockMvc.perform(get("/api/v1/securities/products/1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("AAPL"))
                .andExpect(jsonPath("$.data.week52High").value(260.10));
    }

    // ── SEC-008 ──────────────────────────────────────────────────────────────

    @Test
    void getRanking_상승_상위_200_반환() throws Exception {
        RankingItem item = new RankingItem(
                3L, "NVDA", "엔비디아",
                new BigDecimal("1208.88"), new BigDecimal("4.23"), "2"
        );
        given(securitiesService.getRanking("gainer", 10, null)).willReturn(List.of(item));

        mockMvc.perform(get("/api/v1/securities/products/ranking")
                        .param("type", "gainer")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ticker").value("NVDA"))
                .andExpect(jsonPath("$.data[0].changeRate").value(4.23));
    }
}
