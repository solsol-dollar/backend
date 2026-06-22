package com.shinhan.eclipse.worker.ipo.processor;

import com.shinhan.eclipse.domain.ipo.IpoNews;
import com.shinhan.eclipse.worker.ipo.dto.NewsItem;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class IpoNewsItemConverter implements ItemProcessor<NewsItem, IpoNews> {

    @Override
    public IpoNews process(NewsItem item) {
        return IpoNews.create(
                item.ipoId(),
                item.title(),
                item.source(),
                item.publishedAt(),
                item.url(),
                item.content()
        );
    }
}
