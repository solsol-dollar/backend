package com.shinhan.eclipse.worker.ipo.processor;

import com.shinhan.eclipse.domain.ipo.IpoNews;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IpoNewsKoSummaryProcessor implements ItemProcessor<IpoNews, IpoNews> {

    private final ChatClient chatClient;

    private record KoContent(String titleKo, String summary) {}

    @Override
    public IpoNews process(IpoNews item) {
        if (item.getContent() == null || item.getContent().isBlank()) {
            return null;
        }
        try {
            KoContent result = chatClient.prompt()
                    .system("당신은 IPO 뉴스 한국어 번역 전문가입니다.")
                    .user("다음 영어 IPO 뉴스를 한국어로 변환하세요. "
                            + "titleKo에는 제목을 한국어로 번역하고, "
                            + "summary에는 본문을 2~3문장으로 요약하세요. "
                            + "투자자가 빠르게 읽을 수 있도록 핵심 내용만 담아주세요.\n\n"
                            + "제목: " + item.getTitle() + "\n\n"
                            + "본문:\n" + item.getContent())
                    .call()
                    .entity(KoContent.class);

            if (result == null) return null;
            item.updateTranslation(result.titleKo(), result.summary());
            return item;
        } catch (Exception e) {
            log.warn("번역 실패 [id={}]: {}", item.getId(), e.getMessage());
            return null;
        }
    }
}
