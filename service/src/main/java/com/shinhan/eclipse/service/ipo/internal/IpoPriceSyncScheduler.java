package com.shinhan.eclipse.service.ipo.internal;

import com.shinhan.eclipse.domain.ipo.Ipo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class IpoPriceSyncScheduler {

    private static final String BASE_URL = "https://finnhub.io/api/v1";

    @Value("${finnhub.api-key:}")
    private String apiKey;

    private final IpoRepository ipoRepository;
    private final RestClient finnhubClient = RestClient.builder().baseUrl(BASE_URL).build();

    private record QuoteResponse(BigDecimal c) {}

    @Scheduled(cron = "0 0 7 * * *")
    public void syncCurrentPrices() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("FINNHUB_API_KEY가 설정되지 않았습니다.");
            return;
        }

        List<Ipo> ipos = ipoRepository.findListedIpos();
        List<Ipo> toSave = new ArrayList<>();

        for (Ipo ipo : ipos) {
            try {
                String ticker = ipo.getTicker();
                QuoteResponse quote = finnhubClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/quote")
                                .queryParam("symbol", ticker)
                                .queryParam("token", apiKey)
                                .build())
                        .retrieve()
                        .body(QuoteResponse.class);

                if (quote != null && quote.c() != null && quote.c().compareTo(BigDecimal.ZERO) > 0) {
                    ipo.updateCurrentPrice(quote.c());
                    toSave.add(ipo);
                }
            } catch (Exception e) {
                log.warn("현재가 조회 실패 [{}]: {}", ipo.getTicker(), e.getMessage());
            }
        }

        ipoRepository.saveAll(toSave);
        log.info("현재가 업데이트 완료: {}건 / {}건", toSave.size(), ipos.size());
    }
}
