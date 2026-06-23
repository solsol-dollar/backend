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
public class IpoNewsWriter implements ItemWriter<IpoNews> {

    private final IpoNewsRepository ipoNewsRepository;

    @Override
    public void write(Chunk<? extends IpoNews> chunk) {
        int saved = 0;
        for (IpoNews news : chunk.getItems()) {
            saved += ipoNewsRepository.insertIgnore(
                    news.getIpoId(),
                    news.getTitle(),
                    news.getSource(),
                    news.getPublishedAt(),
                    news.getUrl(),
                    news.getPhase(),
                    news.getContent()
            );
        }
        int skipped = chunk.size() - saved;
        log.info("청크 저장: {}건 완료, {}건 중복 스킵", saved, skipped);
    }
}
