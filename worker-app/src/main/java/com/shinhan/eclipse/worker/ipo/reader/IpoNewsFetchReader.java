package com.shinhan.eclipse.worker.ipo.reader;

import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.worker.ipo.dto.NewsItem;
import com.shinhan.eclipse.worker.ipo.repository.IpoNewsRepository;
import com.shinhan.eclipse.worker.ipo.repository.WorkerIpoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class IpoNewsFetchReader implements ItemReader<NewsItem> {

    private static final String BASE_URL = "https://eodhd.com";
    private static final ZoneId ET = ZoneId.of("America/New_York");
    private static final Set<String> MIN_CONTENT_TICKERS = Set.of("SPCX", "APC");
    private static final DateTimeFormatter EODHD_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    @Value("${eodhd.api-key:}")
    private String apiKey;

    private final WorkerIpoRepository ipoRepository;
    private final IpoNewsRepository ipoNewsRepository;
    private final RestClient restClient = RestClient.builder().baseUrl(BASE_URL).build();

    private Iterator<NewsItem> iterator;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("EODHD_API_KEY가 설정되지 않았습니다. 뉴스 수집을 건너뜁니다.");
            iterator = List.<NewsItem>of().iterator();
            return;
        }
        List<NewsItem> items = fetchAll();
        iterator = items.iterator();
    }

    @Override
    public NewsItem read() {
        return iterator.hasNext() ? iterator.next() : null;
    }

    private List<NewsItem> fetchAll() {
        List<Ipo> ipos = ipoRepository.findByStatus("ACTIVE");
        List<NewsItem> result = new ArrayList<>();

        for (Ipo ipo : ipos) {
            if (ipo.getTicker() == null || ipo.getTicker().isBlank()) continue;
            try {
                List<NewsItem> articles = fetchForTicker(ipo);
                if (articles.isEmpty()) {
                    log.warn("EODHD 0건 [{}]", ipo.getTicker());
                } else {
                    log.info("EODHD [{}]: {}건 수집", ipo.getTicker(), articles.size());
                }
                result.addAll(articles);
            } catch (Exception e) {
                log.warn("EODHD 수집 실패 [{}]: {}", ipo.getTicker(), e.getMessage());
            }
        }

        log.info("전체 수집 완료: {}개 종목, {}건", ipos.size(), result.size());
        return result;
    }

    private List<NewsItem> fetchForTicker(Ipo ipo) {
        LocalDate from = ipoNewsRepository.findMaxPublishedAt(ipo.getId())
                .map(LocalDateTime::toLocalDate)
                .orElse(ipo.getListingDate() != null
                        ? ipo.getListingDate().minusDays(365)
                        : LocalDate.now().minusDays(365));

        LocalDate to = ipo.getListingDate() != null
                ? ipo.getListingDate().minusDays(1)
                : LocalDate.now();

        EodhdArticle[] articles = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/news")
                        .queryParam("s", ipo.getTicker() + ".US")
                        .queryParam("api_token", apiKey)
                        .queryParam("limit", 1000)
                        .queryParam("fmt", "json")
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString())
                        .build())
                .retrieve()
                .body(EodhdArticle[].class);

        if (articles == null || articles.length == 0) return List.of();

        LocalDate windowStart = ipo.getListingDate() != null ? ipo.getListingDate().minusDays(365) : null;
        LocalDate windowEnd   = ipo.getListingDate() != null ? ipo.getListingDate().minusDays(1) : null;

        return Arrays.stream(articles)
                .filter(a -> {
                    if (a.content() == null || a.content().isBlank()) return false;
                    return !MIN_CONTENT_TICKERS.contains(ipo.getTicker()) || a.content().length() >= 500;
                })
                .filter(a -> {
                    if (windowStart == null || a.date() == null) return true;
                    LocalDate articleDate = parseDateET(a.date());
                    if (articleDate == null) return false;
                    return !articleDate.isBefore(windowStart) && !articleDate.isAfter(windowEnd);
                })
                .map(a -> new NewsItem(ipo.getId(), a.title(), a.link(), extractSource(a.link()), parseDate(a.date()), a.content()))
                .toList();
    }

    private String extractSource(String link) {
        if (link == null || link.isBlank()) return null;
        try {
            String host = new java.net.URI(link).getHost();
            if (host == null) return null;
            if (host.startsWith("www.")) host = host.substring(4);
            return switch (host) {
                case "finance.yahoo.com" -> "Yahoo Finance";
                case "nasdaq.com"        -> "Nasdaq";
                case "seekingalpha.com"  -> "Seeking Alpha";
                case "benzinga.com"      -> "Benzinga";
                case "globenewswire.com" -> "GlobeNewsWire";
                case "marketwatch.com"   -> "MarketWatch";
                case "reuters.com"       -> "Reuters";
                case "bloomberg.com"     -> "Bloomberg";
                case "cnbc.com"          -> "CNBC";
                case "wsj.com"           -> "Wall Street Journal";
                case "businesswire.com"  -> "Business Wire";
                case "prnewswire.com"    -> "PR Newswire";
                default                  -> host;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseDate(String date) {
        if (date == null) return null;
        try {
            return OffsetDateTime.parse(date, EODHD_DATE_FORMAT).toLocalDateTime();
        } catch (Exception e) {
            try {
                return OffsetDateTime.parse(date).toLocalDateTime();
            } catch (Exception e2) {
                log.warn("날짜 파싱 실패: {}", date);
                return null;
            }
        }
    }

    private LocalDate parseDateET(String date) {
        if (date == null) return null;
        try {
            return OffsetDateTime.parse(date, EODHD_DATE_FORMAT).atZoneSameInstant(ET).toLocalDate();
        } catch (Exception e) {
            try {
                return OffsetDateTime.parse(date).atZoneSameInstant(ET).toLocalDate();
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private record EodhdArticle(String date, String title, String content, String link) {}
}
