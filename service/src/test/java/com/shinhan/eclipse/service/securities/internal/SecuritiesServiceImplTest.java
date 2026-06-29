package com.shinhan.eclipse.service.securities.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.holding.Holding;
import com.shinhan.eclipse.domain.product.InvestmentProduct;
import com.shinhan.eclipse.service.mypage.MyPageService;
import com.shinhan.eclipse.service.securities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SecuritiesServiceImplTest {

    @Mock ProductRepository          productRepository;
    @Mock HoldingRepository          holdingRepository;
    @Mock FinancialAccountRepository accountRepository;
    @Mock PriceCandleRepository      priceCandleRepository;
    @Mock QuoteCache                 quoteCache;
    @Mock LsRestClient               lsRestClient;
    @Mock KisRestClient              kisRestClient;
    @Mock YahooFinanceClient         yahooFinanceClient;
    @Mock ChatClient                 chatClient;
    @Mock MyPageService              myPageService;

    @InjectMocks SecuritiesServiceImpl service;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "defaultUsdKrw", new BigDecimal("1368.5"));
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private InvestmentProduct productFixture(Long id, String ticker) {
        return InvestmentProduct.ofSeed("OVERSEAS", ticker, ticker + " Inc",
                "NASDAQ", "USD", "Technology");
    }

    private QuoteSnapshot quoteFixture(String ticker) {
        return new QuoteSnapshot(ticker, new BigDecimal("250.00"),
                new BigDecimal("3.00"), new BigDecimal("1.22"), 1000000L, "2", Instant.now());
    }

    // ── SEC-001: listProducts ────────────────────────────────────────────────

    @Test
    void listProducts_종목목록을_캐시_가격과_함께_반환한다() {
        InvestmentProduct p1 = productFixture(1L, "TSLA");
        InvestmentProduct p2 = productFixture(2L, "AAPL");
        given(productRepository.searchProducts(null, null)).willReturn(List.of(p1, p2));
        given(quoteCache.get("TSLA")).willReturn(Optional.of(quoteFixture("TSLA")));
        given(quoteCache.get("AAPL")).willReturn(Optional.empty());
        given(priceCandleRepository.findLatestDailyByProductIds(any())).willReturn(List.of());
        given(priceCandleRepository.findDailyClosePricesForSpark(any(), any())).willReturn(List.of());

        List<ProductListItem> result = service.listProducts(null, null, null, 0, Integer.MAX_VALUE);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).price()).isEqualByComparingTo("250.00");
        assertThat(result.get(1).price()).isNull();
    }

    @Test
    void listProducts_타입과_키워드로_필터링된다() {
        given(productRepository.searchProducts("OVERSEAS", "TSLA")).willReturn(List.of());
        given(priceCandleRepository.findLatestDailyByProductIds(any())).willReturn(List.of());
        given(priceCandleRepository.findDailyClosePricesForSpark(any(), any())).willReturn(List.of());

        List<ProductListItem> result = service.listProducts("OVERSEAS", "TSLA", null, 0, Integer.MAX_VALUE);

        assertThat(result).isEmpty();
        org.mockito.Mockito.verify(productRepository).searchProducts("OVERSEAS", "TSLA");
    }

    // ── SEC-002: getProduct ──────────────────────────────────────────────────

    @Test
    void getProduct_캐시에_시세_있으면_그것을_사용한다() {
        InvestmentProduct p = productFixture(1L, "NVDA");
        given(productRepository.findById(1L)).willReturn(Optional.of(p));
        given(quoteCache.get("NVDA")).willReturn(Optional.of(quoteFixture("NVDA")));

        ProductDetail result = service.getProduct(1L);

        assertThat(result.ticker()).isEqualTo("NVDA");
        assertThat(result.price()).isEqualByComparingTo("250.00");
    }

    @Test
    void getProduct_캐시_미스시_KIS_REST_폴백을_시도한다() {
        InvestmentProduct p = productFixture(1L, "NVDA");
        given(productRepository.findById(1L)).willReturn(Optional.of(p));
        given(quoteCache.get("NVDA")).willReturn(Optional.empty());
        given(kisRestClient.getCurrentPrice("NVDA", "NASDAQ")).willReturn(Optional.empty());

        ProductDetail result = service.getProduct(1L);

        assertThat(result.ticker()).isEqualTo("NVDA");
        assertThat(result.price()).isNull();
    }

    @Test
    void getProduct_KIS_현재가_폴백으로_시세를_반환한다() throws Exception {
        InvestmentProduct p = productFixture(1L, "NVDA");
        given(productRepository.findById(1L)).willReturn(Optional.of(p));
        given(quoteCache.get("NVDA")).willReturn(Optional.empty());

        KisQuoteDto.PriceDetailResponse resp = MAPPER.readValue(
                "{\"rt_cd\":\"0\",\"output\":{\"last\":\"876.50\",\"base\":\"870.00\",\"tvol\":\"5000000\"}}",
                KisQuoteDto.PriceDetailResponse.class);
        given(kisRestClient.getCurrentPrice("NVDA", "NASDAQ")).willReturn(Optional.of(resp));

        ProductDetail result = service.getProduct(1L);

        assertThat(result.ticker()).isEqualTo("NVDA");
        assertThat(result.price()).isEqualByComparingTo("876.50");
    }

    @Test
    void getProduct_존재하지_않는_id면_예외를_던진다() {
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProduct(999L))
                .isInstanceOf(BusinessException.class);
    }

    // ── SEC-003: getOrderBook ────────────────────────────────────────────────

    @Test
    void getOrderBook_KIS_API_응답이_없으면_빈_호가를_반환한다() {
        InvestmentProduct p = productFixture(1L, "AAPL");
        given(productRepository.findById(1L)).willReturn(Optional.of(p));
        given(kisRestClient.getOrderBook("AAPL", "NASDAQ")).willReturn(Optional.empty());

        OrderBookResponse result = service.getOrderBook(1L);

        assertThat(result.ticker()).isEqualTo("AAPL");
        assertThat(result.askLevels()).isEmpty();
        assertThat(result.bidLevels()).isEmpty();
    }

    @Test
    void getOrderBook_KIS_호가_정상_반환한다() throws Exception {
        InvestmentProduct p = productFixture(1L, "AAPL");
        given(productRepository.findById(1L)).willReturn(Optional.of(p));

        String json = """
                {"rt_cd":"0","output1":{"last":"193.50"},
                 "output2":{"pask1":"193.60","pask2":"193.70","pask3":"0","pask4":"0","pask5":"0",
                             "pask6":"0","pask7":"0","pask8":"0","pask9":"0","pask10":"0",
                             "pbid1":"193.40","pbid2":"193.30","pbid3":"0","pbid4":"0","pbid5":"0",
                             "pbid6":"0","pbid7":"0","pbid8":"0","pbid9":"0","pbid10":"0",
                             "vask1":500,"vask2":300,"vask3":0,"vask4":0,"vask5":0,
                             "vask6":0,"vask7":0,"vask8":0,"vask9":0,"vask10":0,
                             "vbid1":700,"vbid2":400,"vbid3":0,"vbid4":0,"vbid5":0,
                             "vbid6":0,"vbid7":0,"vbid8":0,"vbid9":0,"vbid10":0}}
                """;
        KisQuoteDto.AskingPriceResponse resp = MAPPER.readValue(json, KisQuoteDto.AskingPriceResponse.class);
        given(kisRestClient.getOrderBook("AAPL", "NASDAQ")).willReturn(Optional.of(resp));

        OrderBookResponse result = service.getOrderBook(1L);

        assertThat(result.ticker()).isEqualTo("AAPL");
        assertThat(result.price()).isEqualByComparingTo("193.50");
        assertThat(result.askLevels()).hasSize(2);
        assertThat(result.bidLevels()).hasSize(2);
        assertThat(result.askLevels().get(0).price()).isEqualByComparingTo("193.60");
        assertThat(result.bidLevels().get(0).price()).isEqualByComparingTo("193.40");
    }

    // ── SEC-004: getHoldings ─────────────────────────────────────────────────

    @Test
    void getHoldings_보유종목과_손익을_반환한다() {
        Holding holding = Holding.create(1L, 10L, 5, new BigDecimal("200.00"));
        InvestmentProduct p = productFixture(10L, "TSLA");

        given(holdingRepository.findByUserIdAndStatus(1L, "ACTIVE")).willReturn(List.of(holding));
        given(productRepository.findById(10L)).willReturn(Optional.of(p));
        given(quoteCache.get("TSLA")).willReturn(Optional.of(quoteFixture("TSLA")));
        given(accountRepository.findByUserId(1L)).willReturn(List.of());

        HoldingsSummary result = service.getHoldings(1L);

        assertThat(result.holdings()).hasSize(1);
        HoldingItem item = result.holdings().get(0);
        assertThat(item.ticker()).isEqualTo("TSLA");
        assertThat(item.currentPrice()).isEqualByComparingTo("250.00");
        assertThat(item.evaluatedAmount()).isEqualByComparingTo("1250.00");
        // profitLoss = (250 - 200) * 5 = 250
        assertThat(item.profitLoss()).isEqualByComparingTo("250.00");
    }

    @Test
    void getHoldings_시세_없으면_손익계산_null() {
        Holding holding = Holding.create(1L, 10L, 5, new BigDecimal("200.00"));
        InvestmentProduct p = productFixture(10L, "TSLA");

        given(holdingRepository.findByUserIdAndStatus(1L, "ACTIVE")).willReturn(List.of(holding));
        given(productRepository.findById(10L)).willReturn(Optional.of(p));
        given(quoteCache.get("TSLA")).willReturn(Optional.empty());
        given(accountRepository.findByUserId(1L)).willReturn(List.of());

        HoldingsSummary result = service.getHoldings(1L);

        assertThat(result.holdings().get(0).profitLoss()).isNull();
        assertThat(result.holdings().get(0).evaluatedAmount()).isNull();
    }

    // ── SEC-005: getRecommended ──────────────────────────────────────────────

    @Test
    void getRecommended_AI_실패시_기본_추천을_반환한다() {
        InvestmentProduct aapl = productFixture(1L, "AAPL");
        InvestmentProduct msft = productFixture(2L, "MSFT");
        given(productRepository.searchProducts(null, null)).willReturn(List.of(aapl, msft));
        given(myPageService.getLatestProfile(1L)).willThrow(new UnsupportedOperationException("TODO"));
        given(chatClient.prompt()).willThrow(new RuntimeException("AI 장애"));

        List<RecommendedProduct> result = service.getRecommended(1L);

        assertThat(result).extracting(RecommendedProduct::ticker)
                .containsAnyOf("AAPL", "MSFT");
    }
}
