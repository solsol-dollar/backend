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
import java.util.ArrayList;
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
            "FISN", "ELOX", "FRBT", "GMTL",                        // 공모가 없음 or 뉴스 부족
            "PPHC", "AGMB", "PBLS",                                 // 기사 존재 x
            "SGP",                                                   // EODHD 티커 충돌 (호주 Stockland ASX:SGP)
            "FPS", "GENB", "ARXS", "KARD", "ODTX", "YSS", "LCLN", "SHAZ",  // 뉴스 부족 (2건 이하)
            "MOBI", "COAG", "ELMT", "SSMR", "WHK", "EROK"              // 뉴스 부족 (2건 이하)
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
    private record FmpData(String sector) {}

    @Scheduled(cron = "0 0 1 * * *")
    public void sync() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("FINNHUB_API_KEY가 설정되지 않았습니다.");
            return;
        }

        List<Ipo> fetched = fetchFromFinnhub().stream()
                .collect(Collectors.toMap(Ipo::getTicker, i -> i, (a, b) -> a))
                .values().stream().toList();
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
        List<Ipo> result = new ArrayList<>();
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
                        .filter(item -> {
                            BigDecimal[] p = parsePrice(item.price());
                            BigDecimal confirmed = calcConfirmedPrice(p[0], p[1]);
                            return confirmed != null && confirmed.compareTo(BigDecimal.TEN) >= 0;
                        })
                        .map(this::toIpo)
                        .filter(ipo -> ipo.getConfirmedOfferPrice() != null)
                        .filter(ipo -> !"Shell Companies".equals(ipo.getSector()))
                        .filter(ipo -> ipo.getSector() != null
                                || (ipo.getListingDate() != null && ipo.getListingDate().isAfter(LocalDate.now())))
                        .forEach(result::add);
            }
            from = to.plusDays(1);
        }
        return result;
    }

    private Ipo toIpo(IpoItem item) {
        LocalDate listingDate     = item.date() != null ? LocalDate.parse(item.date()) : null;
        BigDecimal[] prices       = parsePrice(item.price());
        String ipoStatus          = "priced".equals(item.status()) ? "OPEN" : "UPCOMING";
        BigDecimal confirmedPrice = calcConfirmedPrice(prices[0], prices[1]);
        FmpData fmpData           = fetchFmpData(item.symbol());

        return Ipo.create(
                item.symbol(),
                item.name(),
                item.exchange(),
                fmpData.sector(),
                listingDate != null ? calcSubscriptionStartDate(listingDate) : null,
                listingDate != null ? listingDate.minusDays(1) : null,
                listingDate,
                listingDate != null ? listingDate.plusDays(1) : null,
                listingDate != null ? calcDepositDate(listingDate) : null,
                prices[0],
                prices[1],
                confirmedPrice,
                BigDecimal.valueOf(100),
                ipoStatus,
                item.numberOfShares(),
                toTradingViewLogoUrl(item.name())
        );
    }

    private BigDecimal calcConfirmedPrice(BigDecimal min, BigDecimal max) {
        if (min != null && max != null) {
            return min.add(max).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        }
        return min != null ? min : max;
    }

    private FmpData fetchFmpData(String ticker) {
        if (fmpApiKey == null || fmpApiKey.isBlank()) return new FmpData(null);
        try {
            FmpProfile[] profiles = fmpClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/profile")
                            .queryParam("symbol", ticker)
                            .queryParam("apikey", fmpApiKey)
                            .build())
                    .retrieve()
                    .body(FmpProfile[].class);
            if (profiles == null || profiles.length == 0) return new FmpData(null);
            FmpProfile p = profiles[0];
            return new FmpData(p.industry() != null ? p.industry() : p.sector());
        } catch (Exception e) {
            log.warn("FMP 데이터 조회 실패: {}", ticker);
            return new FmpData(null);
        }
    }

    private String toTradingViewLogoUrl(String companyName) {
        if (companyName == null) return null;
        String slug = companyName
                .replaceAll("(?i)\\b(inc\\.?|ltd\\.?|corp\\.?|llc\\.?|n\\.v\\.?|\\bnv\\b|pbc|plc|co\\.?)\\b", "")
                .replaceAll("[^a-zA-Z0-9]+", "-")
                .toLowerCase()
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return "https://s3-symbol-logo.tradingview.com/" + slug + "--big.svg";
    }

    private LocalDate calcDepositDate(LocalDate listingDate) {
        LocalDate candidate = listingDate.plusDays(1);
        if (candidate.getDayOfWeek() == DayOfWeek.SATURDAY) return candidate.plusDays(2);
        if (candidate.getDayOfWeek() == DayOfWeek.SUNDAY)   return candidate.plusDays(1);
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
