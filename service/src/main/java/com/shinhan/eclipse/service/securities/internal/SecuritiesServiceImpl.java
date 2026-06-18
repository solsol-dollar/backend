package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.holding.Holding;
import com.shinhan.eclipse.domain.product.InvestmentProduct;
import com.shinhan.eclipse.domain.user.InvestmentProfile;
import com.shinhan.eclipse.service.mypage.MyPageService;
import com.shinhan.eclipse.service.securities.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class SecuritiesServiceImpl implements SecuritiesService {

    private final ProductRepository    productRepository;
    private final HoldingRepository    holdingRepository;
    private final QuoteCache           quoteCache;
    private final LsRestClient         lsRestClient;   // 유지 (LS 코드 보존)
    private final KisRestClient        kisRestClient;
    private final ChatClient           chatClient;
    private final MyPageService        myPageService;

    // ── SEC-001: 종목 목록 ──────────────────────────────────────────────────
    @Override
    public List<ProductListItem> listProducts(String type, String keyword) {
        return productRepository.searchProducts(type, keyword)
                .stream()
                .map(p -> ProductListItem.of(p, quoteCache.get(p.getTicker()).orElse(null)))
                .toList();
    }

    // ── SEC-002: 종목 상세 ──────────────────────────────────────────────────
    @Override
    public ProductDetail getProduct(Long id) {
        InvestmentProduct product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "종목을 찾을 수 없습니다: " + id));

        QuoteSnapshot quote = quoteCache.get(product.getTicker()).orElseGet(() -> {
            // Redis 미스 시 KIS REST 폴백
            return kisRestClient.getCurrentPrice(product.getTicker(), product.getExchangeName())
                    .map(dto -> {
                        KisQuoteDto.PriceDetailResponse.Output o = dto.getOutput();
                        return new QuoteSnapshot(
                                product.getTicker(),
                                o.getLast(),
                                o.diff(),
                                o.rate(),
                                o.volume(),
                                o.sign(),
                                java.time.Instant.now()
                        );
                    })
                    .orElse(null);
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

    // ── SEC-004: 보유 종목 + 손익 ──────────────────────────────────────────
    @Override
    public List<HoldingItem> getHoldings(Long userId) {
        List<Holding> holdings = holdingRepository.findByUserIdAndStatus(userId, "ACTIVE");
        List<HoldingItem> result = new ArrayList<>();

        for (Holding h : holdings) {
            Optional<InvestmentProduct> productOpt = productRepository.findById(h.getProductId());
            if (productOpt.isEmpty()) continue;

            InvestmentProduct p = productOpt.get();
            QuoteSnapshot quote = quoteCache.get(p.getTicker()).orElse(null);

            BigDecimal currentPrice    = quote != null ? quote.price() : null;
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
            }

            result.add(new HoldingItem(
                    h.getId(), p.getId(), p.getTicker(), p.getProductName(),
                    p.getExchangeName(), h.getTotalQuantity(), h.getAveragePrice(),
                    h.getCurrency(), currentPrice, evaluatedAmount, profitLoss, profitLossRate
            ));
        }
        return result;
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
                        return p.map(prod -> new RecommendedProduct(
                                prod.getTicker(), prod.getProductName(), prod.getSector(),
                                prod.getExchangeName(),
                                quoteCache.get(prod.getTicker()).map(QuoteSnapshot::price).orElse(null),
                                rec.reason()
                        )).orElse(null);
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.warn("AI 추천 실패, 기본 추천으로 대체: {}", e.getMessage());
            return fallbackRecommendations(products);
        }
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
                .map(p -> new RecommendedProduct(
                        p.getTicker(), p.getProductName(), p.getSector(), p.getExchangeName(),
                        quoteCache.get(p.getTicker()).map(QuoteSnapshot::price).orElse(null),
                        "대표 우량주/ETF"
                ))
                .toList();
    }

    private record AiRecommendation(String ticker, String reason) {}
}
