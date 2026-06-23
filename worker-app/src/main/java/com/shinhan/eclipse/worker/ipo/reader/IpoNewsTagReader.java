package com.shinhan.eclipse.worker.ipo.reader;

import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.worker.ipo.dto.NewsItem;
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
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class IpoNewsTagReader implements ItemReader<NewsItem> {

    private static final String BASE_URL = "https://eodhd.com";

    private static final Map<String, String> KEYWORD_OVERRIDE = Map.of(
            "OFRM", "Once Upon a Farm",
            "REA",  "Rare Earths Americas",
            "NHP",  "National Healthcare Properties",
            "AADX", "Applied Aerospace & Defense"
    );

    private static final Set<String> TAG_SEARCH_EXCLUDED = Set.of(
            "SPCX"
    );
    private static final Pattern LEGAL_SUFFIX = Pattern.compile(
            "\\b(inc\\.?|corp\\.?|ltd\\.?|llc\\.?|n\\.v\\.?|pbc|plc|co\\.?|" +
            "holdings?|group|technologies?|therapeutics?|sciences?|pharma|" +
            "biosciences?|biomedicines?)\\b[,.]?",
            Pattern.CASE_INSENSITIVE
    );

    @Value("${eodhd.api-key:}")
    private String apiKey;

    private final WorkerIpoRepository ipoRepository;
    private final RestClient restClient = RestClient.builder().baseUrl(BASE_URL).build();

    private Iterator<NewsItem> iterator;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("EODHD_API_KEY가 설정되지 않아 IPO 태그 수집을 건너뜁니다.");
            iterator = List.<NewsItem>of().iterator();
            return;
        }
        iterator = fetchByIpoTag().iterator();
    }

    @Override
    public NewsItem read() {
        return iterator.hasNext() ? iterator.next() : null;
    }

    private static final LocalDate NO_LISTING_DATE_FROM = LocalDate.of(2020, 1, 1);

    private List<NewsItem> fetchByIpoTag() {
        List<Ipo> ipos = ipoRepository.findByStatus("ACTIVE");

        if (ipos.isEmpty()) return List.of();

        Map<String, String> keywordMap = new HashMap<>();
        for (Ipo ipo : ipos) {
            if (TAG_SEARCH_EXCLUDED.contains(ipo.getTicker())) continue;
            String kw = getKeyword(ipo);
            if (kw != null) keywordMap.put(ipo.getTicker(), kw);
        }

        LocalDate today = LocalDate.now();
        LocalDate globalFrom = ipos.stream()
                .map(i -> i.getListingDate() != null
                        ? i.getListingDate().minusDays(365)
                        : NO_LISTING_DATE_FROM)
                .min(Comparator.naturalOrder())
                .orElse(NO_LISTING_DATE_FROM);
        LocalDate globalTo = ipos.stream()
                .map(i -> {
                    if (i.getListingDate() == null) return today;
                    LocalDate postEnd = i.getListingDate().plusDays(90);
                    return postEnd.isAfter(today) ? today : postEnd;
                })
                .max(Comparator.naturalOrder())
                .orElse(today);

        List<EodhdArticle> allArticles;
        try {
            allArticles = fetchAllPages(globalFrom, globalTo);
        } catch (Exception e) {
            log.error("IPO 태그 뉴스 조회 실패, Step 건너뜀: {}", e.getMessage());
            return List.of();
        }
        log.info("IPO 태그 기사 전체: {}건 ({} ~ {})", allArticles.size(), globalFrom, globalTo);

        List<NewsItem> result = new ArrayList<>();
        for (Ipo ipo : ipos) {
            String kw = keywordMap.get(ipo.getTicker());
            if (kw == null) continue;

            LocalDate listingDate = ipo.getListingDate();
            LocalDate windowStart = listingDate != null ? listingDate.minusDays(365) : NO_LISTING_DATE_FROM;
            LocalDate windowEnd   = listingDate != null
                    ? (listingDate.plusDays(90).isAfter(today) ? today : listingDate.plusDays(90))
                    : today;
            String kwLower = kw.toLowerCase();

            int count = 0;
            for (EodhdArticle article : allArticles) {
                if (article.title() == null || !article.title().toLowerCase().contains(kwLower)) continue;
                if (article.content() == null || article.content().contains("Continue Reading")) continue;

                LocalDateTime parsed = EodhdNewsUtil.parseDate(article.date());
                if (parsed == null) continue;
                LocalDate articleDate = EodhdNewsUtil.parseDateET(article.date());
                if (articleDate == null || articleDate.isBefore(windowStart) || articleDate.isAfter(windowEnd)) continue;

                String phase = (listingDate == null || articleDate.isBefore(listingDate)) ? "PRE" : "POST";
                result.add(new NewsItem(ipo.getId(), article.title(),
                        EodhdNewsUtil.extractSource(article.link()), parsed, article.link(), phase, article.content()));
                count++;
            }
            if (count > 0) log.info("IPO 태그 [{}] ({}): {}건", ipo.getTicker(), kw, count);
        }

        log.info("IPO 태그 수집 완료: {}건", result.size());
        return result;
    }

    private static final int MAX_PAGES = 50;

    private List<EodhdArticle> fetchAllPages(LocalDate from, LocalDate to) {
        List<EodhdArticle> result = new ArrayList<>();
        int offset = 0;
        int page = 0;
        while (page < MAX_PAGES) {
            final int currentOffset = offset;
            EodhdArticle[] batch = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/news")
                            .queryParam("t", "IPO")
                            .queryParam("api_token", apiKey)
                            .queryParam("limit", 1000)
                            .queryParam("offset", currentOffset)
                            .queryParam("fmt", "json")
                            .queryParam("from", from.toString())
                            .queryParam("to", to.toString())
                            .build())
                    .retrieve()
                    .body(EodhdArticle[].class);

            if (batch == null || batch.length == 0) break;
            result.addAll(Arrays.asList(batch));
            if (batch.length < 1000) break;
            offset += 1000;
            page++;
        }
        if (page == MAX_PAGES) log.warn("IPO 태그 수집 최대 페이지({}) 도달, 일부 기사 누락 가능", MAX_PAGES);
        return result;
    }

    private String getKeyword(Ipo ipo) {
        if (KEYWORD_OVERRIDE.containsKey(ipo.getTicker())) {
            return KEYWORD_OVERRIDE.get(ipo.getTicker());
        }
        String name = ipo.getCompanyName();
        if (name == null || name.isBlank()) return null;
        String cleaned = LEGAL_SUFFIX.matcher(name).replaceAll("")
                .replaceAll("[&,.]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        String[] parts = cleaned.split("\\s+");
        String keyword = parts.length > 0 ? parts[0] : null;
        return keyword != null && keyword.length() >= 2 ? keyword : null;
    }

    private record EodhdArticle(String date, String title, String content, String link) {}
}
