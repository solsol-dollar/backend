package com.shinhan.eclipse.worker.ipo.writer;

import com.shinhan.eclipse.domain.ipo.IpoNews;
import com.shinhan.eclipse.worker.ipo.repository.IpoNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IpoNewsKoSummaryWriter implements ItemWriter<IpoNews> {

    private final IpoNewsRepository ipoNewsRepository;

    @Override
    public void write(Chunk<? extends IpoNews> chunk) {
        int saved = 0;
        for (IpoNews news : chunk.getItems()) {
            saved += ipoNewsRepository.updateTranslation(news.getId(), news.getTitleKo(), news.getSummary());
        }
        if (saved < chunk.size()) {
            log.warn("한국어 번역 저장 불일치: 대상 {}건, 실제 저장 {}건", chunk.size(), saved);
        } else {
            log.info("한국어 번역 저장: {}건", saved);
        }
    }
}
