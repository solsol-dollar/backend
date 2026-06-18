package com.shinhan.eclipse.worker.candle.job;

import com.shinhan.eclipse.domain.product.InvestmentProduct;
import com.shinhan.eclipse.worker.candle.CandleSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 5년치 초기 적재 잡 (수동 1회 실행).
 *
 * <p>실행 조건: JVM 인수 {@code --job=backfill} 이 있을 때만 동작.
 *
 * <pre>
 * java -jar worker-app.jar --job=backfill
 * </pre>
 *
 * <p>순서: 일봉 → 주봉 → 월봉 (gubun 2 → 3 → 4)
 * 종목 간 200ms 딜레이.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HistoricalBackfillJob implements ApplicationRunner {

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long              DELAY_MS  = 1000L; // KIS rate limit: 1 req/sec

    private final CandleSyncService candleSyncService;

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("job") ||
            !args.getOptionValues("job").contains("backfill")) {
            return;  // backfill 플래그 없으면 스킵
        }

        log.info("===== HistoricalBackfillJob 시작 (5년치 초기 적재) =====");

        LocalDate today   = LocalDate.now();
        LocalDate fiveYearsAgo = today.minusDays(1825);
        String sdate = fiveYearsAgo.format(DATE_FMT);
        String edate = today.format(DATE_FMT);

        List<InvestmentProduct> products = candleSyncService.findActiveProducts();
        log.info("대상 종목 수: {}", products.size());

        // 1단계: 일봉
        log.info("--- [1/3] 일봉 적재 시작 ---");
        int dayTotal = syncAll(products, CandleSyncService.GUBUN_DAY,
                               CandleSyncService.TYPE_DAY, sdate, edate);
        log.info("--- [1/3] 일봉 적재 완료: 총 {}건 ---", dayTotal);

        // 2단계: 주봉
        log.info("--- [2/3] 주봉 적재 시작 ---");
        int weekTotal = syncAll(products, CandleSyncService.GUBUN_WEEK,
                                CandleSyncService.TYPE_WEEK, sdate, edate);
        log.info("--- [2/3] 주봉 적재 완료: 총 {}건 ---", weekTotal);

        // 3단계: 월봉
        log.info("--- [3/3] 월봉 적재 시작 ---");
        int monthTotal = syncAll(products, CandleSyncService.GUBUN_MONTH,
                                 CandleSyncService.TYPE_MONTH, sdate, edate);
        log.info("--- [3/3] 월봉 적재 완료: 총 {}건 ---", monthTotal);

        log.info("===== HistoricalBackfillJob 완료: 일봉={}, 주봉={}, 월봉={} =====",
                dayTotal, weekTotal, monthTotal);
    }

    private int syncAll(List<InvestmentProduct> products, String gubun,
                        String candleType, String sdate, String edate) {
        int total = 0;
        for (InvestmentProduct product : products) {
            try {
                int count = candleSyncService.syncCandlesFull(
                        product, gubun, candleType, sdate, edate, DELAY_MS);
                total += count;
                log.debug("backfill [ticker={}, type={}, 건수={}]",
                        product.getTicker(), candleType, count);
            } catch (Exception e) {
                log.error("backfill 실패 [ticker={}, type={}]: {}",
                        product.getTicker(), candleType, e.getMessage());
            }
            // 종목 간 딜레이 (KIS rate limit 대응)
            try { Thread.sleep(DELAY_MS); } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        return total;
    }
}
