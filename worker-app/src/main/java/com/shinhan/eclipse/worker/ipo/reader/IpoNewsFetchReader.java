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
                .filter(a -> a.content() != null && a.content().length() >= 500)
                .filter(a -> {
                    if (windowStart == null || a.date() == null) return true;
                    LocalDate articleDate = EodhdNewsUtil.parseDateET(a.date());
                    if (articleDate == null) return false;
                    return !articleDate.isBefore(windowStart) && !articleDate.isAfter(windowEnd);
                })
                .map(a -> new NewsItem(ipo.getId(), a.title(), a.link(), EodhdNewsUtil.extractSource(a.link()), EodhdNewsUtil.parseDate(a.date()), a.content()))
                .toList();
    }

    private record EodhdArticle(String date, String title, String content, String link) {}
}
