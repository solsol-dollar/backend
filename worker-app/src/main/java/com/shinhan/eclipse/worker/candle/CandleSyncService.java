package com.shinhan.eclipse.worker.candle;

import com.shinhan.eclipse.domain.product.InvestmentProduct;
import com.shinhan.eclipse.worker.candle.kis.KisDailyPriceResponse;
import com.shinhan.eclipse.worker.candle.kis.WorkerKisRestClient;
import com.shinhan.eclipse.worker.candle.ls.G3204Response;
import com.shinhan.eclipse.worker.candle.ls.WorkerLsRestClient;
import com.shinhan.eclipse.worker.candle.repository.WorkerPriceCandleRepository;
import com.shinhan.eclipse.worker.candle.repository.WorkerProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * 캔들 배치 공통 로직.
 * DailyCandleJob / WeeklyCandleJob / MonthlyCandleJob / HistoricalBackfillJob 이 사용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CandleSyncService {

    private static final String EXCHCD_NASDAQ        = "82";
    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("yyyyMMdd");

    // LS gubun 상수 (유지)
    public static final String GUBUN_DAY   = "2";
    public static final String GUBUN_WEEK  = "3";
    public static final String GUBUN_MONTH = "4";
    public static final String GUBUN_YEAR  = "5";

    // KIS gubn 상수
    public static final String KIS_GUBN_DAY   = "0";
    public static final String KIS_GUBN_WEEK  = "1";
    public static final String KIS_GUBN_MONTH = "2";

    // candleType 상수 (DB 저장 값)
    public static final String TYPE_DAY   = "DAY";
    public static final String TYPE_WEEK  = "WEEK";
    public static final String TYPE_MONTH = "MONTH";
    public static final String TYPE_YEAR  = "YEAR";

    private final WorkerLsRestClient          lsRestClient;   // 유지 (LS 코드 보존)
    private final WorkerKisRestClient         kisRestClient;
    private final WorkerPriceCandleRepository priceCandleRepository;
    private final WorkerProductRepository     productRepository;

    /** 활성 종목 전체 조회 */
    public List<InvestmentProduct> findActiveProducts() {
        return productRepository.findByStatus("ACTIVE");
    }

    /**
     * 단일 날짜 범위의 캔들을 가져와 UPSERT.
     * 연속조회 없이 1회만 호출하는 배치 잡(DailyCandleJob 등)에서 사용.
     *
     * @param gubun LS gubun 상수 (GUBUN_DAY 등) — 내부에서 KIS gubn으로 변환
     */
    @Transactional
    public void syncCandles(InvestmentProduct product, String gubun, String candleType,
                             String sdate, String edate) {
        String kisGubn = toKisGubn(gubun);
        Optional<KisDailyPriceResponse> respOpt = kisRestClient.getDailyCandles(
                product.getTicker(), product.getExchangeName(), kisGubn, edate, "");

        if (respOpt.isEmpty()) {
            log.warn("KIS 기간별시세 응답 없음 [ticker={}, gubn={}, edate={}]",
                    product.getTicker(), kisGubn, edate);
            return;
        }

        KisDailyPriceResponse resp = respOpt.get();
        int count = 0;
        for (KisDailyPriceResponse.DailyCandle c : resp.getCandles()) {
            try {
                upsertKis(product.getId(), candleType, c);
                count++;
            } catch (Exception e) {
                log.warn("캔들 UPSERT 실패 [ticker={}, date={}]: {}",
                        product.getTicker(), c.getXymd(), e.getMessage());
            }
        }
        log.debug("캔들 적재 완료 [ticker={}, type={}, 건수={}]",
                product.getTicker(), candleType, count);
    }

    /**
     * 페이지네이션을 포함한 전체 기간 캔들 적재.
     * HistoricalBackfillJob에서 사용.
     * KIS dailyprice API는 기준일(BYMD)로부터 과거 100건씩 반환하므로
     * 응답의 가장 오래된 날짜를 다음 BYMD로 사용해 페이지네이션한다.
     *
     * @param delayMs 연속 호출 간 딜레이 (밀리초)
     */
    @Transactional
    public int syncCandlesFull(InvestmentProduct product, String gubun, String candleType,
                                String sdate, String edate, long delayMs) {
        String kisGubn    = toKisGubn(gubun);
        String bymd       = edate;
        int    totalCount = 0;
        LocalDate sdateLocal = parseDate(sdate);

        do {
            Optional<KisDailyPriceResponse> respOpt = kisRestClient.getDailyCandles(
                    product.getTicker(), product.getExchangeName(), kisGubn, bymd, "");

            if (respOpt.isEmpty()) {
                log.warn("KIS 기간별시세 페이지 응답 없음 [ticker={}, bymd={}]",
                        product.getTicker(), bymd);
                break;
            }

            KisDailyPriceResponse resp = respOpt.get();
            List<KisDailyPriceResponse.DailyCandle> candles = resp.getCandles();
            if (candles.isEmpty()) break;

            for (KisDailyPriceResponse.DailyCandle c : candles) {
                try {
                    upsertKis(product.getId(), candleType, c);
                    totalCount++;
                } catch (Exception e) {
                    log.warn("캔들 UPSERT 실패 [ticker={}, date={}]: {}",
                            product.getTicker(), c.getXymd(), e.getMessage());
                }
            }

            // 마지막 응답의 가장 오래된 날짜 기준으로 다음 페이지 요청
            String oldestDate = candles.get(candles.size() - 1).getXymd();
            LocalDate oldestLocal = parseDate(oldestDate);
            if (oldestLocal == null || (sdateLocal != null && !oldestLocal.isAfter(sdateLocal))) break;
            if (candles.size() < 100) break; // 마지막 페이지

            bymd = oldestDate;

            try {
                if (delayMs > 0) Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

        } while (true);

        return totalCount;
    }

    private void upsert(Long productId, String candleType, G3204Response.DailyCandle c) {
        LocalDate candleAt = parseDate(c.getDate());
        if (candleAt == null) return;

        priceCandleRepository.upsert(
                productId,
                candleType,
                candleAt,
                parseBD(c.getOpen()),
                parseBD(c.getHigh()),
                parseBD(c.getLow()),
                parseBD(c.getClose()),
                parseLong(c.getVolume()),
                parseBD(c.getAmount()),
                c.getSign()
        );
    }

    private void upsertKis(Long productId, String candleType, KisDailyPriceResponse.DailyCandle c) {
        LocalDate candleAt = parseDate(c.getXymd());
        if (candleAt == null) return;

        priceCandleRepository.upsert(
                productId,
                candleType,
                candleAt,
                parseBD(c.getOpen()),
                parseBD(c.getHigh()),
                parseBD(c.getLow()),
                parseBD(c.getClos()),
                parseLong(c.getTvol()),
                parseBD(c.getTamt()),
                c.getSign()
        );
    }

    /** LS gubun → KIS gubn 변환. */
    private static String toKisGubn(String lsGubun) {
        return switch (lsGubun) {
            case GUBUN_DAY  -> KIS_GUBN_DAY;
            case GUBUN_WEEK -> KIS_GUBN_WEEK;
            default         -> KIS_GUBN_MONTH; // MONTH, YEAR 모두 KIS 월봉으로
        };
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim(), DATE_FMT); }
        catch (DateTimeParseException e) { return null; }
    }

    private BigDecimal parseBD(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(s.trim()); }
        catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private Long parseLong(String s) {
        if (s == null || s.isBlank()) return 0L;
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException e) { return 0L; }
    }
}
