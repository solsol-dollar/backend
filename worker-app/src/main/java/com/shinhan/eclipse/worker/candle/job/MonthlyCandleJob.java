package com.shinhan.eclipse.worker.candle.job;

import com.shinhan.eclipse.domain.product.InvestmentProduct;
import com.shinhan.eclipse.worker.candle.CandleSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 월봉 배치 잡.
 * 매월 1일 09:00 (KST) 실행 — 전월 월봉을 전 종목 UPSERT.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyCandleJob {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CandleSyncService candleSyncService;

    @Scheduled(cron = "0 0 9 1 * *", zone = "Asia/Seoul")
    public void run() {
        // 전월 1일 ~ 말일
        YearMonth prevMonth = YearMonth.now().minusMonths(1);
        LocalDate sdate     = prevMonth.atDay(1);
        LocalDate edate     = prevMonth.atEndOfMonth();

        String sdateStr = sdate.format(DATE_FMT);
        String edateStr = edate.format(DATE_FMT);

        log.info("MonthlyCandleJob 시작: {}~{}", sdateStr, edateStr);

        List<InvestmentProduct> products = candleSyncService.findActiveProducts();
        int success = 0;
        int failure = 0;

        for (InvestmentProduct product : products) {
            try {
                candleSyncService.syncCandles(
                        product,
                        CandleSyncService.GUBUN_MONTH,
                        CandleSyncService.TYPE_MONTH,
                        sdateStr, edateStr
                );
                success++;
            } catch (Exception e) {
                failure++;
                log.error("MonthlyCandleJob 종목 실패 [ticker={}]: {}", product.getTicker(), e.getMessage());
            }
        }

        log.info("MonthlyCandleJob 완료: 성공={}, 실패={}", success, failure);
    }
}
