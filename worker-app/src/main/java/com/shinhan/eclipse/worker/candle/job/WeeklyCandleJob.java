package com.shinhan.eclipse.worker.candle.job;

import com.shinhan.eclipse.domain.product.InvestmentProduct;
import com.shinhan.eclipse.worker.candle.CandleSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 주봉 배치 잡.
 * 매주 토요일 08:00 (KST) 실행 — 전주 주봉을 전 종목 UPSERT.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyCandleJob {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final CandleSyncService candleSyncService;

    @Scheduled(cron = "0 0 8 * * SAT", zone = "Asia/Seoul")
    public void run() {
        // 전주 월요일 ~ 금요일
        LocalDate today    = LocalDate.now(KST);                       // 토요일
        LocalDate lastFri  = today.minusDays(1);                       // 금요일 (주봉 마지막 거래일)
        LocalDate lastMon  = lastFri.minusDays(4);                     // 월요일
        String sdate = lastMon.format(DATE_FMT);
        String edate = lastFri.format(DATE_FMT);

        log.info("WeeklyCandleJob 시작: {}~{}", sdate, edate);

        List<InvestmentProduct> products = candleSyncService.findActiveProducts();
        int success = 0;
        int failure = 0;

        for (InvestmentProduct product : products) {
            try {
                candleSyncService.syncCandles(
                        product,
                        CandleSyncService.GUBUN_WEEK,
                        CandleSyncService.TYPE_WEEK,
                        sdate, edate
                );
                success++;
            } catch (Exception e) {
                failure++;
                log.error("WeeklyCandleJob 종목 실패 [ticker={}]: {}", product.getTicker(), e.getMessage());
            }
        }

        log.info("WeeklyCandleJob 완료: 성공={}, 실패={}", success, failure);
    }
}
