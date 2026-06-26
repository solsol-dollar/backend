package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.holding.Holding;
import com.shinhan.eclipse.domain.product.InvestmentProduct;
import com.shinhan.eclipse.domain.product.PriceCandle;
import com.shinhan.eclipse.domain.user.InvestmentProfile;
import com.shinhan.eclipse.service.mypage.MyPageService;
import com.shinhan.eclipse.service.securities.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class SecuritiesServiceImpl implements SecuritiesService {

    private final ProductRepository         productRepository;
    private final HoldingRepository         holdingRepository;
    private final FinancialAccountRepository accountRepository;
    private final PriceCandleRepository     priceCandleRepository;
    private final QuoteCache                quoteCache;
    private final LsRestClient              lsRestClient;   // 유지 (LS 코드 보존)
    private final KisRestClient             kisRestClient;
    private final ChatClient                chatClient;
    private final MyPageService             myPageService;

    @Value("${eclipse.fx.usd-krw:1368.5}")
    private BigDecimal usdKrw;

    // ── SEC-001: 종목 목록 ──────────────────────────────────────────────────
    @Override
    public List<ProductListItem> listProducts(String type, String keyword, String sort) {
        List<InvestmentProduct> products = productRepository.searchProducts(type, keyword);

        List<Long> ids = products.stream().map(InvestmentProduct::getId).toList();

        // 최신 일봉 캔들 벌크 조회 (거래량/거래대금 보강용)
        Map<Long, com.shinhan.eclipse.domain.product.PriceCandle> candleMap =
                priceCandleRepository.findLatestDailyByProductIds(ids)
                        .stream()
                        .collect(Collectors.toMap(
                                com.shinhan.eclipse.domain.product.PriceCandle::getProductId,
                                c -> c
                        ));

        // 스파크라인용 최근 30일 종가 (product_id, close_price)
        LocalDate sparkFrom = LocalDate.now().minusDays(30);
        Map<Long, List<BigDecimal>> sparkMap =
                priceCandleRepository.findDailyClosePricesForSpark(ids, sparkFrom)
                        .stream()
                        .collect(Collectors.groupingBy(
                                row -> ((Number) row[0]).longValue(),
                                Collectors.mapping(row -> (BigDecimal) row[1], Collectors.toList())
                        ));

        Comparator<ProductListItem> comparator = buildSortComparator(sort);
        return products.stream()
                .map(p -> ProductListItem.of(
                        p,
                        quoteCache.get(p.getTicker()).orElse(null),
                        candleMap.get(p.getId()),
                        sparkMap.getOrDefault(p.getId(), List.of())
                ))
                .sorted(comparator)
                .toList();
    }

    private Comparator<ProductListItem> buildSortComparator(String sort) {
        Comparator<Long> nullSafeId = Comparator.nullsLast(Comparator.naturalOrder());
        if (sort == null) return Comparator.comparing(ProductListItem::id, nullSafeId);
        return switch (sort) {
            case "TRADING_VALUE" -> Comparator.comparing(
                    ProductListItem::tradeAmount,
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
            case "TRADING_VOLUME" -> Comparator.comparing(
                    ProductListItem::volume,
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
            case "RISE" -> Comparator.comparing(
                    ProductListItem::changeRate,
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
            case "FALL" -> Comparator.comparing(
                    ProductListItem::changeRate,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            default -> Comparator.comparing(ProductListItem::id, nullSafeId);
        };
    }

    // ── SEC-002: 종목 상세 ──────────────────────────────────────────────────
    @Override
    public ProductDetail getProduct(Long id) {
        InvestmentProduct product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "종목을 찾을 수 없습니다: " + id));

        QuoteSnapshot quote = quoteCache.get(product.getTicker()).orElseGet(() -> {
            QuoteSnapshot fetched = kisRestClient.getCurrentPrice(product.getTicker(), product.getExchangeName())
                    .map(dto -> kisToSnapshot(product.getTicker(), dto))
                    .orElse(null);
            if (fetched != null) quoteCache.put(product.getTicker(), fetched);
            return fetched;
        });

        return ProductDetail.of(product, quote);
    }

    // ── SEC-003: 호가 ───────────────────────────────────────────────────────
    @Override
    public OrderBookResponse getOrderBook(Long id) {
        InvestmentProduct product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "종목을 찾을 수 없습니다: " + id));

        return kisRestClient.getOrderBook(product.getTicker(), product.getExchangeName())
                .filter(dto -> dto.getOutput2() != null)
                .map(dto -> {
                    KisQuoteDto.AskingPriceResponse.Output2 b = dto.getOutput2();
                    BigDecimal price = dto.getOutput1() != null ? dto.getOutput1().getLast() : null;

                    List<BigDecimal> askPrices  = b.askPrices();
                    List<Long>       askVolumes = b.askVolumes();
                    List<BigDecimal> bidPrices  = b.bidPrices();
                    List<Long>       bidVolumes = b.bidVolumes();

                    List<OrderBookResponse.Level> ask = java.util.stream.IntStream.range(0, 10)
                            .filter(i -> askPrices.get(i) != null && askPrices.get(i).compareTo(BigDecimal.ZERO) > 0)
                            .mapToObj(i -> new OrderBookResponse.Level(askPrices.get(i),
                                    askVolumes.get(i) != null ? askVolumes.get(i) : 0L))
                            .toList();

                    List<OrderBookResponse.Level> bid = java.util.stream.IntStream.range(0, 10)
                            .filter(i -> bidPrices.get(i) != null && bidPrices.get(i).compareTo(BigDecimal.ZERO) > 0)
                            .mapToObj(i -> new OrderBookResponse.Level(bidPrices.get(i),
                                    bidVolumes.get(i) != null ? bidVolumes.get(i) : 0L))
                            .toList();

                    return new OrderBookResponse(product.getTicker(), price, ask, bid);
                })
                .orElse(new OrderBookResponse(product.getTicker(), null, List.of(), List.of()));
    }

    // ── SEC-004: 보유 종목 + 손익 (래퍼 포함) ────────────────────────────────
    @Override
    public HoldingsSummary getHoldings(Long userId) {
        List<Holding> holdings = holdingRepository.findByUserIdAndStatus(userId, "ACTIVE");
        List<HoldingItem> holdingItems = new ArrayList<>();

        BigDecimal totalCurrentValueUsd = BigDecimal.ZERO;
        BigDecimal totalCostUsd         = BigDecimal.ZERO;
        BigDecimal dayChangeUsd         = BigDecimal.ZERO;

        for (Holding h : holdings) {
            Optional<InvestmentProduct> productOpt = productRepository.findById(h.getProductId());
            if (productOpt.isEmpty()) continue;

            InvestmentProduct p = productOpt.get();
            QuoteSnapshot quote = quoteCache.get(p.getTicker()).orElse(null);

            BigDecimal currentPrice    = quote != null ? quote.price()  : null;
            BigDecimal evaluatedAmount = null;
            BigDecimal profitLoss      = null;
            BigDecimal profitLossRate  = null;

            if (currentPrice != null) {
                evaluatedAmount = currentPrice.multiply(BigDecimal.valueOf(h.getTotalQuantity()));
                profitLoss      = currentPrice.subtract(h.getAveragePrice())
                        .multiply(BigDecimal.valueOf(h.getTotalQuantity()));
                profitLossRate  = currentPrice.subtract(h.getAveragePrice())
                        .divide(h.getAveragePrice(), 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                totalCurrentValueUsd = totalCurrentValueUsd.add(evaluatedAmount);
            }

            totalCostUsd = totalCostUsd.add(
                    h.getAveragePrice().multiply(BigDecimal.valueOf(h.getTotalQuantity())));

            BigDecimal change = quote != null ? quote.change() : BigDecimal.ZERO;
            dayChangeUsd = dayChangeUsd.add(
                    change.multiply(BigDecimal.valueOf(h.getTotalQuantity())));

            holdingItems.add(new HoldingItem(
                    h.getId(), p.getId(), p.getTicker(), p.getProductName(),
                    p.getProductType(), p.getExchangeName(), h.getTotalQuantity(), h.getAveragePrice(),
                    h.getCurrency(), currentPrice, evaluatedAmount, profitLoss, profitLossRate
            ));
        }

        List<FinancialAccount> accounts = accountRepository.findByUserId(userId);
        BigDecimal cashUsd = accounts.stream()
                .filter(a -> "USD".equals(a.getCurrency()))
                .map(FinancialAccount::getBalance)
                .findFirst()
                .orElse(BigDecimal.ZERO);
        BigDecimal cashKrw = cashUsd.multiply(usdKrw).setScale(0, RoundingMode.HALF_UP);

        return new HoldingsSummary(
                totalCurrentValueUsd,
                totalCostUsd,
                dayChangeUsd,
                cashUsd,
                cashKrw,
                holdingItems
        );
    }

    // ── SEC-005: AI 추천 ───────────────────────────────────────────────────
    @Override
    public List<RecommendedProduct> getRecommended(Long userId) {
        String riskType = safeGetRiskType(userId);
        List<InvestmentProduct> products = productRepository.searchProducts(null, null);

        String productJson = products.stream()
                .limit(30)
                .map(p -> "{\"ticker\":\"%s\",\"name\":\"%s\",\"sector\":\"%s\"}"
                        .formatted(p.getTicker(), p.getProductName(), p.getSector()))
                .reduce((a, b) -> a + "," + b)
                .map(s -> "[" + s + "]")
                .orElse("[]");

        String prompt = """
                You are a financial advisor. The user's risk profile is: %s.
                From the following NASDAQ 100 stocks and ETFs, recommend exactly 5 products.
                Return ONLY a JSON array with this format (no explanation):
                [{"ticker":"AAPL","reason":"..."},...]

                Available products: %s
                """.formatted(riskType, productJson);

        try {
            List<AiRecommendation> aiResult = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(new ParameterizedTypeReference<>() {});

            if (aiResult == null || aiResult.isEmpty()) return fallbackRecommendations(products);

            return aiResult.stream()
                    .map(rec -> {
                        Optional<InvestmentProduct> p = products.stream()
                                .filter(prod -> prod.getTicker().equalsIgnoreCase(rec.ticker()))
                                .findFirst();
                        return p.map(prod -> {
                            QuoteSnapshot q = quoteCache.get(prod.getTicker()).orElse(null);
                            return new RecommendedProduct(
                                    prod.getId(), prod.getTicker(), prod.getProductName(),
                                    prod.getProductType(), prod.getSector(), prod.getExchangeName(),
                                    q != null ? q.price()      : null,
                                    q != null ? q.changeRate() : null,
                                    q != null ? q.sign()       : null,
                                    rec.reason()
                            );
                        }).orElse(null);
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.warn("AI 추천 실패, 기본 추천으로 대체: {}", e.getMessage());
            return fallbackRecommendations(products);
        }
    }

    // ── B-01: 시장 지수 ────────────────────────────────────────────────────
    @Override
    public List<MarketIndex> getMarketIndices() {
        boolean marketOpen = MarketHoursUtil.isUsMarketOpen();
        MarketIndex spy = resolveIndexSnapshot("SPY", "NYSE", marketOpen);
        MarketIndex qqq = resolveIndexSnapshot("QQQ", "NAS",  marketOpen);
        MarketIndex usdKrwIndex = new MarketIndex("USD/KRW", usdKrw, BigDecimal.ZERO, BigDecimal.ZERO, false, false);

        return List.of(spy, qqq, usdKrwIndex);
    }

    private static final java.util.Map<String, String> INDEX_DISPLAY_NAMES = java.util.Map.of(
            "SPY", "S&P 500",
            "QQQ", "나스닥"
    );

    private MarketIndex resolveIndexSnapshot(String ticker, String exchange, boolean marketOpen) {
        String displayName = INDEX_DISPLAY_NAMES.getOrDefault(ticker, ticker);
        QuoteSnapshot snapshot = quoteCache.get(ticker).orElseGet(() ->
                kisRestClient.getCurrentPrice(ticker, exchange)
                        .map(dto -> kisToSnapshot(ticker, dto))
                        .orElse(null)
        );
        if (snapshot == null) {
            return new MarketIndex(displayName, null, BigDecimal.ZERO, BigDecimal.ZERO, false, marketOpen);
        }
        return new MarketIndex(
                displayName,
                snapshot.price(),
                snapshot.change(),
                snapshot.changeRate(),
                !"-".equals(snapshot.sign()) && snapshot.change().compareTo(BigDecimal.ZERO) >= 0,
                marketOpen
        );
    }

    private QuoteSnapshot kisToSnapshot(String ticker, KisQuoteDto.PriceDetailResponse dto) {
        KisQuoteDto.PriceDetailResponse.Output o = dto.getOutput();
        return new QuoteSnapshot(ticker, o.getLast(), o.diff(), o.rate(), o.volume(), o.sign(), Instant.now());
    }

    private String safeGetRiskType(Long userId) {
        try {
            InvestmentProfile profile = myPageService.getLatestProfile(userId);
            return profile != null ? profile.getRiskType() : "MODERATE";
        } catch (Exception e) {
            return "MODERATE";
        }
    }

    private List<RecommendedProduct> fallbackRecommendations(List<InvestmentProduct> products) {
        return products.stream()
                .filter(p -> List.of("AAPL", "MSFT", "NVDA", "QQQ", "SOXX").contains(p.getTicker()))
                .map(p -> {
                    QuoteSnapshot q = quoteCache.get(p.getTicker()).orElse(null);
                    return new RecommendedProduct(
                            p.getId(), p.getTicker(), p.getProductName(),
                            p.getProductType(), p.getSector(), p.getExchangeName(),
                            q != null ? q.price()      : null,
                            q != null ? q.changeRate() : null,
                            q != null ? q.sign()       : null,
                            "대표 우량주/ETF"
                    );
                })
                .toList();
    }

    private record AiRecommendation(String ticker, String reason) {}

    // ── SEC-007: 종목 통계 ─────────────────────────────────────────────────
    @Override
    public ProductStats getProductStats(Long id) {
        InvestmentProduct product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "종목을 찾을 수 없습니다: " + id));

        LocalDate today = LocalDate.now();
        LocalDate week52Ago = today.minusWeeks(52);

        BigDecimal week52High = priceCandleRepository.findWeek52High(id, week52Ago).orElse(null);
        BigDecimal week52Low  = priceCandleRepository.findWeek52Low(id, week52Ago).orElse(null);

        // 최신 종가
        Optional<com.shinhan.eclipse.domain.product.PriceCandle> latestOpt =
                priceCandleRepository.findFirstByProductIdAndCandleTypeOrderByCandleAtDesc(id, "DAY");
        BigDecimal latestClose = latestOpt.map(c -> c.getClosePrice()).orElse(null);

        // 기간별 수익률 계산
        java.util.LinkedHashMap<String, BigDecimal> returns = new java.util.LinkedHashMap<>();
        String[] periods  = {"1M",   "3M",   "6M",    "1Y"};
        LocalDate[] froms = {
            today.minusMonths(1), today.minusMonths(3),
            today.minusMonths(6), today.minusYears(1)
        };
        for (int i = 0; i < periods.length; i++) {
            if (latestClose != null) {
                Optional<com.shinhan.eclipse.domain.product.PriceCandle> baseOpt =
                        priceCandleRepository.findFirstByProductIdAndCandleTypeAndCandleAtGreaterThanEqualOrderByCandleAtAsc(id, "DAY", froms[i]);
                if (baseOpt.isPresent()) {
                    BigDecimal baseClose = baseOpt.get().getClosePrice();
                    if (baseClose.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal ret = latestClose.subtract(baseClose)
                                .divide(baseClose, 4, java.math.RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                        returns.put(periods[i], ret);
                    }
                }
            }
        }

        return new ProductStats(product.getTicker(), week52High, week52Low, returns);
    }

    // ── SEC-008: 종목 랭킹 ─────────────────────────────────────────────────
    @Override
    public List<RankingItem> getRanking(String type, int limit) {
        List<InvestmentProduct> products = productRepository.searchProducts(null, null);

        // 캐시 히트 종목으로 우선 랭킹 구성
        List<RankingItem> cached = products.stream()
                .map(p -> {
                    QuoteSnapshot q = quoteCache.get(p.getTicker()).orElse(null);
                    if (q == null) return null;
                    return new RankingItem(p.getId(), p.getTicker(), p.getProductName(),
                            q.price(), q.changeRate(), q.sign());
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        // 캐시 미스 시 price_candles 최신 일봉 fallback
        if (cached.isEmpty()) {
            List<Long> ids = products.stream().map(InvestmentProduct::getId).toList();
            java.util.Map<Long, PriceCandle> latestByProduct = priceCandleRepository
                    .findLatestDailyByProductIds(ids).stream()
                    .collect(java.util.stream.Collectors.toMap(
                            PriceCandle::getProductId, c -> c));
            return products.stream()
                    .map(p -> {
                        PriceCandle c = latestByProduct.get(p.getId());
                        if (c == null) return null;
                        BigDecimal change = c.getClosePrice().subtract(c.getOpenPrice());
                        BigDecimal rate = c.getOpenPrice().compareTo(BigDecimal.ZERO) == 0
                                ? BigDecimal.ZERO
                                : change.divide(c.getOpenPrice(), 4, java.math.RoundingMode.HALF_UP)
                                        .multiply(new BigDecimal("100"));
                        String sign = rate.compareTo(BigDecimal.ZERO) > 0 ? "2"
                                : rate.compareTo(BigDecimal.ZERO) < 0 ? "5" : "3";
                        return new RankingItem(p.getId(), p.getTicker(), p.getProductName(),
                                c.getClosePrice(), rate, sign);
                    })
                    .filter(java.util.Objects::nonNull)
                    .sorted(buildRankingComparator(type))
                    .limit(limit)
                    .toList();
        }

        return cached.stream()
                .sorted(buildRankingComparator(type))
                .limit(limit)
                .toList();
    }

    private Comparator<RankingItem> buildRankingComparator(String type) {
        Comparator<BigDecimal> nullsLast = Comparator.nullsLast(Comparator.naturalOrder());
        return switch (type == null ? "gainer" : type) {
            case "loser"  -> Comparator.comparing(RankingItem::changeRate, nullsLast);
            case "volume" -> Comparator.comparing(
                    (RankingItem r) -> quoteCache.get(r.ticker())
                            .map(QuoteSnapshot::volume).orElse(0L),
                    Comparator.<Long>nullsLast(Comparator.reverseOrder())
            );
            default       -> Comparator.comparing(RankingItem::changeRate,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }
}
