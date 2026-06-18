package com.shinhan.eclipse.service.securities.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.domain.product.InvestmentProduct;
import com.shinhan.eclipse.domain.product.PriceCandle;
import com.shinhan.eclipse.service.securities.ChartResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ChartServiceImplTest {

    @Mock ProductRepository              productRepository;
    @Mock PriceCandleRepository          priceCandleRepository;
    @Mock LsRestClient                   lsRestClient;   // 유지 (LS 코드 보존)
    @Mock KisRestClient                  kisRestClient;
    @Mock StringRedisTemplate            redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @Spy  ObjectMapper                   objectMapper = new ObjectMapper();

    @InjectMocks ChartServiceImpl chartService;

    // ── getChart: 종목 없음 ───────────────────────────────────────────────────

    @Test
    void getChart_없는_종목이면_BusinessException() {
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chartService.getChart(999L, "1M"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("999");
    }

    // ── getChart: 잘못된 period ───────────────────────────────────────────────

    @Test
    void getChart_잘못된_period이면_BusinessException() {
        given(productRepository.findById(1L)).willReturn(Optional.of(makeProduct("AAPL")));

        assertThatThrownBy(() -> chartService.getChart(1L, "INVALID"))
                .isInstanceOf(BusinessException.class);
    }

    // ── getChart 1W: DB 조회 ──────────────────────────────────────────────────

    @Test
    void getChart_1W_DB에서_일봉_반환() {
        InvestmentProduct product = makeProduct("AAPL");
        PriceCandle candle = makePriceCandle(null, "DAY", LocalDate.of(2026, 6, 10));

        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(priceCandleRepository.findByProductIdAndCandleTypeAndCandleAtBetween(
                any(), eq("DAY"), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of(candle));

        ChartResponse response = chartService.getChart(1L, "1W");

        assertThat(response.getTicker()).isEqualTo("AAPL");
        assertThat(response.getCandleType()).isEqualTo("DAY");
        assertThat(response.getCandles()).hasSize(1);
        assertThat(response.getCandles().get(0).getDate()).isEqualTo("20260610");
        assertThat(response.getCandles().get(0).getTime()).isNull();
    }

    // ── getChart 3M: 주봉 ─────────────────────────────────────────────────────

    @Test
    void getChart_3M_주봉_반환() {
        InvestmentProduct product = makeProduct("NVDA");
        PriceCandle candle = makePriceCandle(null, "WEEK", LocalDate.of(2026, 4, 4));

        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(priceCandleRepository.findByProductIdAndCandleTypeAndCandleAtBetween(
                any(), eq("WEEK"), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of(candle));

        ChartResponse response = chartService.getChart(1L, "3M");

        assertThat(response.getCandleType()).isEqualTo("WEEK");
        assertThat(response.getCandles()).hasSize(1);
    }

    // ── getChart 1Y: 월봉 ─────────────────────────────────────────────────────

    @Test
    void getChart_1Y_월봉_반환() {
        InvestmentProduct product = makeProduct("MSFT");
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(priceCandleRepository.findByProductIdAndCandleTypeAndCandleAtBetween(
                any(), eq("MONTH"), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of());

        ChartResponse response = chartService.getChart(1L, "1Y");

        assertThat(response.getCandleType()).isEqualTo("MONTH");
        assertThat(response.getCandles()).isEmpty();
    }

    // ── getChart 1D: Redis HIT ────────────────────────────────────────────────

    @Test
    void getChart_1D_Redis_캐시_HIT() throws Exception {
        InvestmentProduct product = makeProduct("AAPL");
        String today = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String redisKey = "chart:min:5:AAPL:" + today;

        List<ChartResponse.CandleItem> cachedCandles = List.of(
                new ChartResponse.CandleItem("20260617", "093000",
                        BigDecimal.valueOf(193), BigDecimal.valueOf(194),
                        BigDecimal.valueOf(192), BigDecimal.valueOf(193), 100000L, null)
        );
        String json = new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .writeValueAsString(cachedCandles);

        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(redisKey)).willReturn(json);

        ChartResponse response = chartService.getChart(1L, "1D");

        assertThat(response.getCandleType()).isEqualTo("MINUTE");
        assertThat(response.getCandles()).hasSize(1);
        assertThat(response.getCandles().get(0).getTime()).isEqualTo("093000");
        verify(kisRestClient, never()).getMinuteCandles(any(), any());
    }

    // ── getChart 1D: Redis MISS → KIS 폴백 ──────────────────────────────────

    @Test
    void getChart_1D_Redis_MISS_KIS_폴백() throws Exception {
        InvestmentProduct product = makeProduct("AAPL");

        KisChartDto.MinuteChartResponse kisResp = new ObjectMapper()
                .readValue("{\"rt_cd\":\"0\",\"output2\":[]}", KisChartDto.MinuteChartResponse.class);

        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(anyString())).willReturn(null);
        given(kisRestClient.getMinuteCandles(eq("AAPL"), any()))
                .willReturn(Optional.of(kisResp));

        ChartResponse response = chartService.getChart(1L, "1D");

        assertThat(response.getCandleType()).isEqualTo("MINUTE");
        assertThat(response.getCandles()).isEmpty();
    }

    @Test
    void getChart_1D_KIS_분봉_데이터_반환() throws Exception {
        InvestmentProduct product = makeProduct("AAPL");
        String json = """
                {"rt_cd":"0","output2":[
                  {"xymd":"20260618","xhms":"093000","open":"193.1","high":"194.0","low":"192.9","last":"193.8","evol":"12345"},
                  {"xymd":"20260618","xhms":"093500","open":"193.8","high":"195.0","low":"193.6","last":"194.7","evol":"9876"}
                ]}""";
        KisChartDto.MinuteChartResponse kisResp = new ObjectMapper().readValue(json, KisChartDto.MinuteChartResponse.class);

        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(anyString())).willReturn(null);
        given(kisRestClient.getMinuteCandles(eq("AAPL"), any()))
                .willReturn(Optional.of(kisResp));

        ChartResponse response = chartService.getChart(1L, "1D");

        assertThat(response.getCandleType()).isEqualTo("MINUTE");
        assertThat(response.getCandles()).hasSize(2);
        assertThat(response.getCandles().get(0).getTime()).isEqualTo("093000");
        assertThat(response.getCandles().get(0).getClose()).isEqualByComparingTo("193.8");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private InvestmentProduct makeProduct(String ticker) {
        return InvestmentProduct.ofSeed("OVERSEAS", ticker, ticker + " Inc",
                "NASDAQ", "USD", "Technology");
        // id는 null이지만 테스트에서 eq(1L) 매칭이 필요하므로 별도 mock 설정
    }

    private PriceCandle makePriceCandle(Long productId, String candleType, LocalDate candleAt) {
        return PriceCandle.of(productId, candleType, candleAt,
                BigDecimal.valueOf(180), BigDecimal.valueOf(185),
                BigDecimal.valueOf(179), BigDecimal.valueOf(183),
                50000000L, BigDecimal.valueOf(9250000000.0), "2");
    }
}
