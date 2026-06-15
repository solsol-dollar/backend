package com.shinhan.eclipse.worker.writer;

import com.shinhan.eclipse.domain.ipo.IpoNews;
import com.shinhan.eclipse.worker.repository.IpoNewsRepository;
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
        ipoNewsRepository.saveAll(chunk.getItems());
        log.info("Saved {} IPO news articles", chunk.size());
    }
}
