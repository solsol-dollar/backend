package com.shinhan.eclipse.worker.candle.job;

import com.shinhan.eclipse.domain.product.InvestmentProduct;
import com.shinhan.eclipse.worker.candle.CandleSyncService;
import com.shinhan.eclipse.worker.candle.repository.WorkerPriceCandleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 캔들 Gap Fill 잡.
 *
 * <p>각 종목·캔들타입별로 DB의 마지막 날짜를 확인 후, 그 날짜부터 오늘까지만 요청한다.
 * 캔들이 아예 없는 종목은 5년치 전체 백필로 폴백한다.
 *
 * <ul>
 *   <li>latestDate == null → sdate = today - 5년 (신규 종목)</li>
 *   <li>latestDate >= yesterday → skip (이미 최신)</li>
 *   <li>그 외 → sdate = latestDate (마지막 날짜 포함 재요청 — 당일 가격 갱신 보장)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CandleGapFillJob {

    private static final DateTimeFormatter DATE_FMT      = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId            KST           = ZoneId.of("Asia/Seoul");
    private static final long              LOOKBACK_DAYS = 365L * 5;
    private static final long              DELAY_MS      = 1000L;

    private final CandleSyncService           candleSyncService;
    private final WorkerPriceCandleRepository priceCandleRepository;

    private record TypeConfig(String gubun, String candleType) {}

    private static final List<TypeConfig> CANDLE_TYPES = List.of(
            new TypeConfig(CandleSyncService.GUBUN_DAY,   CandleSyncService.TYPE_DAY),
            new TypeConfig(CandleSyncService.GUBUN_WEEK,  CandleSyncService.TYPE_WEEK),
            new TypeConfig(CandleSyncService.GUBUN_MONTH, CandleSyncService.TYPE_MONTH)
    );

    public void run(List<InvestmentProduct> products) {
        LocalDate today      = LocalDate.now(KST);
        LocalDate yesterday  = today.minusDays(1);
        LocalDate fiveYrsAgo = today.minusDays(LOOKBACK_DAYS);
        String    edate      = today.format(DATE_FMT);

        Map<String, LocalDate> latestMap = buildLatestMap();
        log.info("CandleGapFillJob 시작: 대상 종목={}, 기준일={}", products.size(), today);

        int filled = 0, skipped = 0, failed = 0;

        for (InvestmentProduct product : products) {
            for (TypeConfig tc : CANDLE_TYPES) {
                LocalDate latest = latestMap.get(key(product.getId(), tc.candleType()));

                LocalDate sdateLocal;
                if (latest == null) {
                    sdateLocal = fiveYrsAgo;
                } else if (!latest.isBefore(yesterday)) {
                    skipped++;
                    continue;
                } else {
                    sdateLocal = latest;
                }

                String sdate = sdateLocal.format(DATE_FMT);
                log.debug("GapFill [ticker={}, type={}, {}~{}]",
                        product.getTicker(), tc.candleType(), sdate, edate);
                try {
                    candleSyncService.syncCandlesFull(
                            product, tc.gubun(), tc.candleType(), sdate, edate, DELAY_MS);
                    filled++;
                } catch (Exception e) {
                    log.error("GapFill 실패 [ticker={}, type={}]: {}",
                            product.getTicker(), tc.candleType(), e.getMessage());
                    failed++;
                }
            }

            try {
                Thread.sleep(DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("CandleGapFillJob 완료: filled={}, skipped={}, failed={}", filled, skipped, failed);
    }

    private Map<String, LocalDate> buildLatestMap() {
        List<Object[]> rows = priceCandleRepository.findLatestCandleDates();
        Map<String, LocalDate> map = new HashMap<>();
        for (Object[] row : rows) {
            Long      productId  = ((Number) row[0]).longValue();
            String    candleType = (String) row[1];
            LocalDate latest     = ((java.sql.Date) row[2]).toLocalDate();
            map.put(key(productId, candleType), latest);
        }
        return map;
    }

    private String key(Long productId, String candleType) {
        return productId + ":" + candleType;
    }
}
