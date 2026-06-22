package com.shinhan.eclipse.service.ipo.internal;

import com.shinhan.eclipse.domain.ipo.Ipo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
class FinnhubSyncScheduler {

    private static final String BASE_URL = "https://finnhub.io/api/v1";
    private static final String FMP_BASE_URL = "https://financialmodelingprep.com/stable";

    private static final Set<String> EXCLUDED_TICKERS = Set.of(
            "CAES", "RACC",                                         // 껍데기 금융 지주사
            "YSS", "SHAZ", "EROC", "HMH", "EROK",                 // 정체불명
            "WHK", "SSMR", "GMTL", "SBMT", "REA", "ELMT",         // 소규모 광물/채굴
            "FCBM", "AGBK",                                         // 지역 소형 은행
            "YSWY", "PPHC"                                          // 낮은 관심 기타
    );

    @Value("${finnhub.api-key:}")
    private String apiKey;

    @Value("${fmp.api-key:}")
    private String fmpApiKey;

    private final IpoRepository ipoRepository;
    private final RestClient finnhubClient = RestClient.builder().baseUrl(BASE_URL).build();
    private final RestClient fmpClient = RestClient.builder().baseUrl(FMP_BASE_URL).build();

    private record IpoResponse(List<IpoItem> ipoCalendar) {}
    private record IpoItem(String symbol, String name, String date, String exchange,
                           String price, String status, Long numberOfShares, Long totalSharesValue) {}
    private record FmpProfile(String sector, String industry) {}

    @Scheduled(cron = "0 0 1 * * *")
    public void sync() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("FINNHUB_API_KEY가 설정되지 않았습니다.");
            return;
        }

        List<Ipo> fetched = fetchFromFinnhub();
        if (fetched.isEmpty()) return;

        List<String> tickers = fetched.stream().map(Ipo::getTicker).toList();
        Set<String> existingTickers = ipoRepository.findExistingTickers(tickers);

        List<Ipo> newIpos = fetched.stream()
                .filter(ipo -> !existingTickers.contains(ipo.getTicker()))
                .collect(Collectors.toList());

        if (!newIpos.isEmpty()) {
            ipoRepository.saveAll(newIpos);
        }

        log.info("IPO 동기화 완료: {}건 신규 / {}건 조회", newIpos.size(), fetched.size());
    }

    private List<Ipo> fetchFromFinnhub() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end   = LocalDate.of(2026, 12, 31);
        List<Ipo> result = new java.util.ArrayList<>();
        LocalDate from = start;
        while (from.isBefore(end)) {
            LocalDate to = from.plusMonths(4).isAfter(end) ? end : from.plusMonths(4);
            log.info("Finnhub IPO 캘린더 조회: {} ~ {}", from, to);

            LocalDate finalFrom = from;
            LocalDate finalTo   = to;
            IpoResponse response = finnhubClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/calendar/ipo")
                            .queryParam("from", finalFrom.toString())
                            .queryParam("to", finalTo.toString())
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(IpoResponse.class);

            if (response != null && response.ipoCalendar() != null) {
                response.ipoCalendar().stream()
                        .filter(item -> "expected".equals(item.status()) || "priced".equals(item.status()))
                        .filter(item -> item.date() != null && !item.date().isBlank())
                        .filter(item -> item.price() != null && !item.price().isBlank())
                        .filter(item -> !isSpac(item))
                        .filter(item -> !EXCLUDED_TICKERS.contains(item.symbol()))
                        .map(this::toIpo)
                        .filter(ipo -> ipo.getConfirmedOfferPrice() != null)
                        .filter(ipo -> ipo.getConfirmedOfferPrice().compareTo(BigDecimal.TEN) >= 0)
                        .filter(ipo -> !"Shell Companies".equals(ipo.getSector()))
                        .filter(ipo -> ipo.getSector() != null
                                || (ipo.getListingDate() != null && ipo.getListingDate().isAfter(LocalDate.now())))
                        .forEach(result::add);
            }
            from = to;
        }
        return result;
    }

    private Ipo toIpo(IpoItem item) {
        LocalDate listingDate  = item.date() != null ? LocalDate.parse(item.date()) : null;
        BigDecimal[] prices    = parsePrice(item.price());
        String ipoStatus       = "priced".equals(item.status()) ? "OPEN" : "UPCOMING";
        BigDecimal confirmedPrice = calcConfirmedPrice(prices[0], prices[1]);
        String sector          = fetchSector(item.symbol());

        return Ipo.create(
                item.symbol(),
                item.name(),
                item.exchange(),
                sector,
                listingDate != null ? calcSubscriptionStartDate(listingDate) : null,
                listingDate != null ? listingDate.minusDays(1) : null,
                listingDate,
                listingDate != null ? listingDate.plusDays(1) : null,
                listingDate != null ? calcDepositDate(listingDate) : null,
                prices[0],
                prices[1],
                confirmedPrice,
                BigDecimal.valueOf(100),
                null, // totalAllocableShares: 중개사 계약값, 외부 API에 없음 — 운영자가 별도 입력
                ipoStatus
        );
    }

    private BigDecimal calcConfirmedPrice(BigDecimal min, BigDecimal max) {
        if (min != null && max != null) {
            return min.add(max).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        }
        return min != null ? min : max;
    }

    private String fetchSector(String ticker) {
        if (fmpApiKey == null || fmpApiKey.isBlank()) return null;
        try {
            FmpProfile[] profiles = fmpClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/profile")
                            .queryParam("symbol", ticker)
                            .queryParam("apikey", fmpApiKey)
                            .build())
                    .retrieve()
                    .body(FmpProfile[].class);
            if (profiles == null || profiles.length == 0) return null;
            FmpProfile p = profiles[0];
            return p.industry() != null ? p.industry() : p.sector();
        } catch (Exception e) {
            log.warn("FMP 섹터 조회 실패: {}", ticker);
            return null;
        }
    }

    private LocalDate calcDepositDate(LocalDate listingDate) {
        LocalDate candidate = listingDate.plusDays(1);
        if (candidate.getDayOfWeek() == DayOfWeek.SATURDAY) {
            candidate = candidate.plusDays(2);
        } else if (candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    private LocalDate calcSubscriptionStartDate(LocalDate listingDate) {
        int daysBack = ThreadLocalRandom.current().nextInt(12, 18);
        LocalDate candidate = listingDate.minusDays(daysBack);
        if (candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidate = candidate.minusDays(1);
        }
        return candidate;
    }

    private boolean isSpac(IpoItem item) {
        boolean isPricedAt10 = "10.00".equals(item.price()) || "10".equals(item.price());
        String name = item.name() != null ? item.name().toLowerCase() : "";
        boolean hasAcquisitionName = name.contains("acquisition") || name.contains("capital corp");
        boolean hasUnitTicker = item.symbol() != null && item.symbol().endsWith("U");
        return isPricedAt10 && (hasAcquisitionName || hasUnitTicker);
    }

    private BigDecimal[] parsePrice(String price) {
        if (price == null || price.isBlank()) return new BigDecimal[]{null, null};
        if (price.contains("-")) {
            String[] parts = price.split("-", 2);
            if (parts.length < 2) return new BigDecimal[]{null, null};
            try {
                return new BigDecimal[]{new BigDecimal(parts[0].trim()), new BigDecimal(parts[1].trim())};
            } catch (NumberFormatException e) {
                return new BigDecimal[]{null, null};
            }
        }
        try {
            BigDecimal p = new BigDecimal(price.trim());
            return new BigDecimal[]{p, p};
        } catch (NumberFormatException e) {
            return new BigDecimal[]{null, null};
        }
    }
}
