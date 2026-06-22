package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.service.securities.QuoteReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class IndexRefreshScheduler {

    // ticker, KIS exchangeName
    private static final List<String[]> INDEX_TICKERS = List.of(
            new String[]{"SPY", "NYSE"},
            new String[]{"QQQ", "NASDAQ"}
    );

    private final KisRestClient            kisRestClient;
    private final ApplicationEventPublisher publisher;

    /** 장 중 60초마다 SPY/QQQ 현재가를 KIS REST로 갱신 */
    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    void refreshIndices() {
        if (!MarketHoursUtil.isUsMarketOpen()) {
            log.debug("장 마감 — 지수 갱신 스킵");
            return;
        }
        for (String[] entry : INDEX_TICKERS) {
            String ticker = entry[0], exchange = entry[1];
            try {
                kisRestClient.getCurrentPrice(ticker, exchange).ifPresent(dto -> {
                    var o = dto.getOutput();
                    publisher.publishEvent(QuoteReceivedEvent.of(
                            ticker,
                            o.getLast(),
                            o.diff(),
                            o.rate(),
                            o.volume(),
                            o.sign()
                    ));
                    log.debug("지수 갱신: {} = {}", ticker, o.getLast());
                });
            } catch (Exception e) {
                log.warn("지수 갱신 실패 [{}]: {}", ticker, e.getMessage());
            }
        }
    }

    /** 장 마감 직후(ET 16:01) 종가 1회 스냅샷 */
    @Scheduled(cron = "0 1 16 * * MON-FRI", zone = "America/New_York")
    void snapshotClose() {
        for (String[] entry : INDEX_TICKERS) {
            String ticker = entry[0], exchange = entry[1];
            try {
                kisRestClient.getCurrentPrice(ticker, exchange).ifPresent(dto -> {
                    var o = dto.getOutput();
                    publisher.publishEvent(QuoteReceivedEvent.of(
                            ticker,
                            o.getLast(),
                            o.diff(),
                            o.rate(),
                            o.volume(),
                            o.sign()
                    ));
                    log.info("종가 스냅샷: {} = {}", ticker, o.getLast());
                });
            } catch (Exception e) {
                log.warn("종가 스냅샷 실패 [{}]: {}", ticker, e.getMessage());
            }
        }
    }
}
