package com.shinhan.eclipse.worker.ipo.reader;

import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.worker.ipo.dto.NewsItem;
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
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
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

    private List<NewsItem> fetchByIpoTag() {
        List<Ipo> ipos = ipoRepository.findByStatus("ACTIVE").stream()
                .filter(i -> i.getListingDate() != null)
                .toList();

        if (ipos.isEmpty()) return List.of();

        Map<String, String> keywordMap = new HashMap<>();
        for (Ipo ipo : ipos) {
            if (TAG_SEARCH_EXCLUDED.contains(ipo.getTicker())) continue;
            String kw = getKeyword(ipo);
            if (kw != null) keywordMap.put(ipo.getTicker(), kw);
        }

        LocalDate globalFrom = ipos.stream()
                .map(i -> i.getListingDate().minusDays(365))
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now().minusDays(365));
        LocalDate globalTo = ipos.stream()
                .map(i -> i.getListingDate().minusDays(1))
                .max(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        List<EodhdArticle> allArticles = fetchAllPages(globalFrom, globalTo);
        log.info("IPO 태그 기사 전체: {}건 ({} ~ {})", allArticles.size(), globalFrom, globalTo);

        List<NewsItem> result = new ArrayList<>();
        for (Ipo ipo : ipos) {
            String kw = keywordMap.get(ipo.getTicker());
            if (kw == null) continue;

            LocalDate windowStart = ipo.getListingDate().minusDays(365);
            LocalDate windowEnd   = ipo.getListingDate().minusDays(1);
            String kwLower = kw.toLowerCase();

            int count = 0;
            for (EodhdArticle article : allArticles) {
                if (article.title() == null || !article.title().toLowerCase().contains(kwLower)) continue;
                if (article.content() == null || article.content().length() < 500) continue;

                LocalDateTime parsed = EodhdNewsUtil.parseDate(article.date());
                if (parsed == null) continue;
                LocalDate articleDate = EodhdNewsUtil.parseDateET(article.date());
                if (articleDate == null || articleDate.isBefore(windowStart) || articleDate.isAfter(windowEnd)) continue;

                result.add(new NewsItem(ipo.getId(), article.title(), article.link(),
                        EodhdNewsUtil.extractSource(article.link()), parsed, article.content()));
                count++;
            }
            if (count > 0) log.info("IPO 태그 [{}] ({}): {}건", ipo.getTicker(), kw, count);
        }

        log.info("IPO 태그 수집 완료: {}건", result.size());
        return result;
    }

    private List<EodhdArticle> fetchAllPages(LocalDate from, LocalDate to) {
        List<EodhdArticle> result = new ArrayList<>();
        int offset = 0;
        while (true) {
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
        }
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
        return parts.length > 0 && !parts[0].isBlank() ? parts[0] : null;
    }

    private record EodhdArticle(String date, String title, String content, String link) {}
}
