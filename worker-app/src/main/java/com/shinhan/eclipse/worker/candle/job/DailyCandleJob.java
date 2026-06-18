package com.shinhan.eclipse.worker.candle.job;

import com.shinhan.eclipse.domain.product.InvestmentProduct;
import com.shinhan.eclipse.worker.candle.CandleSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 일봉 배치 잡.
 * 매일 07:00 (KST, 월~토) 실행 — 전날 일봉을 전 종목 UPSERT.
 * 토요일이면 금요일(최근 거래일) 일봉을 적재한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyCandleJob {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final CandleSyncService candleSyncService;

    @Scheduled(cron = "0 0 7 * * MON-SAT", zone = "Asia/Seoul")
    public void run() {
        LocalDate targetDate = resolveTargetDate();
        String dateStr = targetDate.format(DATE_FMT);
        log.info("DailyCandleJob 시작: targetDate={}", dateStr);

        List<InvestmentProduct> products = candleSyncService.findActiveProducts();
        int success = 0;
        int failure = 0;

        for (InvestmentProduct product : products) {
            try {
                candleSyncService.syncCandles(
                        product,
                        CandleSyncService.GUBUN_DAY,
                        CandleSyncService.TYPE_DAY,
                        dateStr, dateStr
                );
                success++;
            } catch (Exception e) {
                failure++;
                log.error("DailyCandleJob 종목 실패 [ticker={}]: {}", product.getTicker(), e.getMessage());
            }
        }

        log.info("DailyCandleJob 완료: 성공={}, 실패={}, targetDate={}", success, failure, dateStr);
    }

    /**
     * 전날 날짜 계산.
     * 월요일이면 전 거래일은 금요일(-3일)이 아니라 어제(일요일)가 아닌 금요일.
     * 배치가 월~토 실행이므로:
     * - 토요일 실행 → 금요일
     * - 나머지 → 어제
     */
    private LocalDate resolveTargetDate() {
        LocalDate today = LocalDate.now(KST);
        if (today.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return today.minusDays(1); // 금요일
        }
        return today.minusDays(1);
    }
}
