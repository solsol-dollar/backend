package com.shinhan.eclipse.service.card;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpendingReportScheduler {

    private final SpendingReportService spendingReportService;

    @Scheduled(cron = "0 0 10 1 * *", zone = "Asia/Seoul")
    public void generateMonthlyReport() {
        YearMonth prev = YearMonth.now().minusMonths(1);
        log.info("[소비리포트] 자동 실행: {}년 {}월", prev.getYear(), prev.getMonthValue());
        spendingReportService.generateAll(prev.getYear(), prev.getMonthValue());
    }
}
