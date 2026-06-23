package com.shinhan.eclipse.worker.ipo.reader;

import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.worker.ipo.dto.NewsItem;
import com.shinhan.eclipse.worker.ipo.repository.IpoNewsRepository;
import com.shinhan.eclipse.worker.ipo.repository.WorkerIpoRepository;
import com.shinhan.eclipse.worker.ipo.util.EodhdNewsUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class IpoNewsFetchReader implements ItemReader<NewsItem> {

    private static final String BASE_URL = "https://eodhd.com";

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
        LocalDate today = LocalDate.now();
        LocalDate listingDate = ipo.getListingDate();

        LocalDate windowStart = listingDate != null ? listingDate.minusDays(365) : today.minusDays(365);
        LocalDate windowEnd   = listingDate != null
                ? listingDate.plusDays(90).isAfter(today) ? today : listingDate.plusDays(90)
                : today;

        LocalDate from = ipoNewsRepository.findMaxPublishedAt(ipo.getId())
                .map(LocalDateTime::toLocalDate)
                .orElse(windowStart);

        if (!from.isBefore(windowEnd)) {
            log.debug("EODHD [{}]: 수집 범위 없음 (from={}, to={})", ipo.getTicker(), from, windowEnd);
            return List.of();
        }

        List<EodhdArticle> allArticles = new ArrayList<>();
        int offset = 0;
        int page = 0;
        final int MAX_PAGES = 20;
        final String fromStr = from.toString();
        final String toStr = windowEnd.toString();
        final String tickerUs = ipo.getTicker() + ".US";
        while (page < MAX_PAGES) {
            final int currentOffset = offset;
            EodhdArticle[] batch = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/news")
                            .queryParam("s", tickerUs)
                            .queryParam("api_token", apiKey)
                            .queryParam("limit", 1000)
                            .queryParam("offset", currentOffset)
                            .queryParam("fmt", "json")
                            .queryParam("from", fromStr)
                            .queryParam("to", toStr)
                            .build())
                    .retrieve()
                    .body(EodhdArticle[].class);

            if (batch == null || batch.length == 0) break;
            allArticles.addAll(Arrays.asList(batch));
            if (batch.length < 1000) break;
            offset += 1000;
            page++;
        }
        if (page == MAX_PAGES) log.warn("EODHD [{}]: 최대 페이지({}) 도달, 일부 기사 누락 가능", ipo.getTicker(), MAX_PAGES);

        if (allArticles.isEmpty()) return List.of();

        return allArticles.stream()
                .filter(a -> a.title() != null && !a.title().isBlank())
                .filter(a -> a.content() != null && !a.content().contains("Continue Reading"))
                .filter(a -> {
                    if (a.date() == null) return true;
                    LocalDate articleDate = EodhdNewsUtil.parseDateET(a.date());
                    if (articleDate == null) return false;
                    return !articleDate.isBefore(windowStart) && !articleDate.isAfter(windowEnd);
                })
                .map(a -> {
                    LocalDateTime publishedAt = EodhdNewsUtil.parseDate(a.date());
                    LocalDate articleDate = EodhdNewsUtil.parseDateET(a.date());
                    String phase = (listingDate == null || articleDate == null ||
                                    articleDate.isBefore(listingDate)) ? "PRE" : "POST";
                    return new NewsItem(ipo.getId(), a.title(),
                            EodhdNewsUtil.extractSource(a.link()), publishedAt, a.link(), phase, a.content());
                })
                .toList();
    }

    private record EodhdArticle(String date, String title, String content, String link) {}
}
