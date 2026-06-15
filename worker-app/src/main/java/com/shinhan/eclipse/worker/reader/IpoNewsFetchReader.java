package com.shinhan.eclipse.worker.reader;

import com.shinhan.eclipse.worker.dto.NewsItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
public class IpoNewsFetchReader implements ItemReader<NewsItem> {

    private Iterator<NewsItem> iterator;

    // @BeforeStep 으로 Job 시작 시 한 번 호출
    public void init() {
        List<NewsItem> items = fetchFromFinnhub();
        this.iterator = items.iterator();
    }

    @Override
    public NewsItem read() {
        if (iterator == null) {
            init();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }

    private List<NewsItem> fetchFromFinnhub() {
        // TODO: Finnhub /company-news API 호출 → 활성 IPO 목록 조회 → 각 ticker 뉴스 수집
        // - 중복 URL은 IpoNewsRepository.existsByUrl() 로 필터링 후 제외
        // - from/to 날짜: 최근 7일
        log.info("Fetching IPO news from Finnhub");
        return List.of();
    }
}
