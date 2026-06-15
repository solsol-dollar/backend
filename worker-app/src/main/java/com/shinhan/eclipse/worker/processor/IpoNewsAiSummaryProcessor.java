package com.shinhan.eclipse.worker.processor;

import com.shinhan.eclipse.domain.ipo.IpoNews;
import com.shinhan.eclipse.worker.dto.NewsItem;
import com.shinhan.eclipse.worker.dto.NewsSummaryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IpoNewsAiSummaryProcessor implements ItemProcessor<NewsItem, IpoNews> {

    private final ChatClient chatClient;

    @Override
    public IpoNews process(NewsItem item) {
        String summary = generateSummary(item);

        IpoNews news = new IpoNews();
        // TODO: IpoNews setter 또는 생성자 구현 후 필드 설정
        // news.setIpoId(item.ipoId());
        // news.setTitle(item.title());
        // news.setUrl(item.url());
        // news.setSource(item.source());
        // news.setPublishedAt(item.publishedAt());
        // news.setSummary(summary);
        return news;
    }

    private String generateSummary(NewsItem item) {
        try {
            NewsSummaryResult result = chatClient.prompt()
                    .user(u -> u.text("""
                            다음 IPO 관련 뉴스 기사 제목을 한국어로 2-3문장 요약해줘.
                            JSON 형식으로만 응답: {"summary": "요약 내용"}

                            제목: {title}
                            """)
                            .param("title", item.title()))
                    .call()
                    .entity(NewsSummaryResult.class);

            return result != null ? result.summary() : null;
        } catch (Exception e) {
            // AI 실패 시 summary null 로 저장 — Step Skip 정책으로 처리
            log.warn("AI summary generation failed for news: {}", item.title(), e);
            return null;
        }
    }
}
