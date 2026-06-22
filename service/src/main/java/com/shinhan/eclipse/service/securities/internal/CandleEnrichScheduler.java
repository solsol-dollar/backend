package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.domain.product.InvestmentProduct;
import com.shinhan.eclipse.domain.product.PriceCandle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class CandleEnrichScheduler {

    private final ProductRepository     productRepository;
    private final PriceCandleRepository priceCandleRepository;
    private final KisRestClient         kisRestClient;

    /** 서버 시작 30초 후 첫 적재 (ProductDataInitializer 시딩 완료 대기) */
    @Scheduled(fixedDelay = Long.MAX_VALUE, initialDelay = 30_000)
    void onStartup() {
        if (!kisRestClient.isConfigured()) {
            log.warn("KIS 미설정 — 일봉 초기 적재 스킵");
            return;
        }
        log.info("일봉 초기 적재 시작 (시작 30초 후)");
        syncAll();
    }

    /** 매일 KST 06:00 (미국 ET 약 17:00 이후) 장 마감 후 적재 */
    @Scheduled(cron = "0 0 6 * * TUE-SAT", zone = "Asia/Seoul")
    void dailySync() {
        if (!kisRestClient.isConfigured()) return;
        log.info("일봉 일간 동기화 시작");
        syncAll();
    }

    @Transactional
    public void syncAll() {
        List<InvestmentProduct> products = productRepository.findAll();
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int success = 0, skip = 0, fail = 0;

        for (InvestmentProduct p : products) {
            try {
                boolean saved = kisRestClient
                        .getLatestDailyCandle(p.getTicker(), p.getExchangeName())
                        .map(dto -> upsert(p.getId(), dto))
                        .orElse(false);
                if (saved) success++; else skip++;
                Thread.sleep(650); // ~1.5 req/sec
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("일봉 적재 실패 [{}]: {}", p.getTicker(), e.getMessage());
                fail++;
            }
        }
        log.info("일봉 적재 완료: 성공={} 스킵={} 실패={}", success, skip, fail);
    }

    private boolean upsert(Long productId, KisDailyPriceDto.DailyCandle dto) {
        if (dto.getClos() == null || dto.getClos().isBlank()) return false;

        LocalDate candleAt;
        try {
            candleAt = LocalDate.parse(dto.getXymd(), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (Exception e) {
            return false;
        }

        PriceCandle existing = priceCandleRepository
                .findByProductIdAndCandleTypeAndCandleAt(productId, "DAY", candleAt)
                .orElse(null);

        if (existing != null) {
            // 이미 존재 — 스킵 (당일 중복 방지)
            return false;
        }

        PriceCandle candle = PriceCandle.of(
                productId, "DAY", candleAt,
                safeOrDefault(dto.getOpen(),  dto.closeBD()),
                safeOrDefault(dto.getHigh(),  dto.closeBD()),
                safeOrDefault(dto.getLow(),   dto.closeBD()),
                dto.closeBD(),
                dto.volLong(),
                dto.amtBD(),
                dto.getSign()
        );
        priceCandleRepository.save(candle);
        return true;
    }

    private java.math.BigDecimal safeOrDefault(String s, java.math.BigDecimal fallback) {
        if (s == null || s.isBlank()) return fallback;
        try { return new java.math.BigDecimal(s.trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
}
