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
                    .system("당신은 금융·경제 뉴스 한국어 요약 전문가입니다.\n"
                            + "규칙:\n"
                            + "- 한국경제·매일경제 보도체로 작성하세요.\n"
                            + "- 번역투를 피하고 자연스러운 한국어 문장으로 작성하세요.\n"
                            + "- 회사명·티커심볼·거래소명은 영어 원문 그대로 유지하세요.\n"
                            + "- 달러 금액과 주식 수는 숫자로 표현하세요. (예: $18, $200M, 10,000,000주)\n"
                            + "- 주관사·인수단 명단은 딜 규모가 특별히 크거나 핵심일 때만 언급하고, 일반적인 경우 생략하세요.\n"
                            + "- 본문에 없는 정보는 절대 추가하지 마세요.\n"
                            + "- 응답은 반드시 순수 JSON만 출력하세요. 마크다운 코드블록 없이.")
                    .user("다음 금융 뉴스를 분석해 아래 JSON 형식으로만 응답하세요.\n\n"
                            + "{\n"
                            + "  \"titleKo\": \"자연스러운 한국어 제목 (회사명은 영문 유지)\",\n"
                            + "  \"summary\": \"본문 정보만 사용해 2~3문장 요약.\"\n"
                            + "}\n\n"
                            + "summary 작성 순서:\n"
                            + "1. 핵심 실적 또는 이벤트 (무슨 일이 있었나)\n"
                            + "2. 시장·애널리스트 반응 또는 배경 (왜 의미 있나)\n"
                            + "3. 향후 전망 또는 가이던스 (앞으로 어떻게 되나)\n"
                            + "※ 수치를 나열하되, 각 문장은 수치의 의미나 맥락으로 마무리하세요.\n"
                            + "  (예: '~를 기록하며 시장 예상을 상회했다', '~로 성장세를 이어갔다')\n"
                            + "※ 본문에 없는 항목은 생략하고 있는 정보만 자연스럽게 연결하세요.\n\n"
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
