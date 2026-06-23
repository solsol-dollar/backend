package com.shinhan.eclipse.worker.ipo.reader;

import com.shinhan.eclipse.domain.ipo.IpoNews;
import com.shinhan.eclipse.worker.ipo.repository.IpoNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class IpoNewsKoSummaryReader implements ItemReader<IpoNews> {

    private final IpoNewsRepository ipoNewsRepository;
    private Iterator<IpoNews> iterator;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        List<IpoNews> targets = ipoNewsRepository.findAllForTranslation();
        log.info("한국어 요약 대상: {}건", targets.size());
        iterator = targets.iterator();
    }

    @Override
    public IpoNews read() {
        return iterator.hasNext() ? iterator.next() : null;
    }
}
