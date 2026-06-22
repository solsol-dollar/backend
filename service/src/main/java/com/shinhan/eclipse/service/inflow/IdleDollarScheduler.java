package com.shinhan.eclipse.service.inflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdleDollarScheduler {

    private final IdleDollarService idleDollarService;

    @Scheduled(cron = "0 0 11 * * *", zone = "Asia/Seoul")
    public void detectIdleDollars() {
        log.info("유휴 달러 감지 잡 시작");
        idleDollarService.detectAll();
    }
}