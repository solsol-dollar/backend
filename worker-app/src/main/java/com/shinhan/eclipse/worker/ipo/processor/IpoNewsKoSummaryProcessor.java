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
                    .system("당신은 IPO 뉴스 한국어 요약 전문가입니다.\n"
                            + "규칙:\n"
                            + "- 한국 경제 신문(한국경제·매일경제) 보도체로 작성하세요.\n"
                            + "- 번역투 표현을 피하고 자연스러운 한국어 문장으로 작성하세요.\n"
                            + "- 회사명·티커심볼·거래소명은 영어 원문 그대로 유지하세요.\n"
                            + "- 달러 금액과 주식 수는 숫자로 표현하세요. (예: $18, $200M, 10,000,000주)\n"
                            + "- 본문에 없는 정보는 절대 추가하지 마세요.")
                    .user("다음 IPO 뉴스를 분석해 한국어로 변환하세요.\n\n"
                            + "titleKo: 제목을 자연스러운 한국어로 번역 (회사명은 영문 유지)\n"
                            + "summary: 본문에 있는 정보만 사용해 2~3문장으로 요약. "
                            + "없는 항목은 생략하고 있는 정보만 자연스럽게 연결하세요.\n\n"
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
