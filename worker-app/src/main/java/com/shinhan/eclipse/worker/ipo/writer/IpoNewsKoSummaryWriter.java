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
        for (IpoNews news : chunk.getItems()) {
            ipoNewsRepository.updateTranslation(news.getId(), news.getTitleKo(), news.getSummary());
        }
        log.info("한국어 번역 저장: {}건", chunk.size());
    }
}
