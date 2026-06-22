package com.shinhan.eclipse.worker.ipo.job;

import com.shinhan.eclipse.domain.ipo.IpoNews;
import com.shinhan.eclipse.worker.ipo.dto.NewsItem;
import com.shinhan.eclipse.worker.ipo.processor.IpoNewsItemConverter;
import com.shinhan.eclipse.worker.ipo.processor.IpoNewsKoSummaryProcessor;
import com.shinhan.eclipse.worker.ipo.reader.IpoNewsFetchReader;
import com.shinhan.eclipse.worker.ipo.reader.IpoNewsKoSummaryReader;
import com.shinhan.eclipse.worker.ipo.reader.IpoNewsTagReader;
import com.shinhan.eclipse.worker.ipo.writer.IpoNewsKoSummaryWriter;
import com.shinhan.eclipse.worker.ipo.writer.IpoNewsWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class IpoNewsSyncJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final IpoNewsFetchReader ipoNewsFetchReader;
    private final IpoNewsItemConverter ipoNewsItemConverter;
    private final IpoNewsWriter ipoNewsWriter;
    private final IpoNewsTagReader ipoNewsTagReader;
    private final IpoNewsKoSummaryReader ipoNewsKoSummaryReader;
    private final IpoNewsKoSummaryProcessor ipoNewsKoSummaryProcessor;
    private final IpoNewsKoSummaryWriter ipoNewsKoSummaryWriter;

    @Bean
    public Job ipoNewsSyncJob() {
        return new JobBuilder("ipoNewsSyncJob", jobRepository)
                .start(ipoNewsSyncStep())
                .next(ipoNewsKoSummaryStep())
                .build();
    }

    /** 뉴스 fetch만 실행 (번역 제외) */
    @Bean
    public Job ipoNewsFetchOnlyJob() {
        return new JobBuilder("ipoNewsFetchOnlyJob", jobRepository)
                .start(ipoNewsSyncStep())
                .next(ipoNewsTagStep())
                .build();
    }

    @Bean
    public Step ipoNewsSyncStep() {
        return new StepBuilder("ipoNewsSyncStep", jobRepository)
                .<NewsItem, IpoNews>chunk(50, transactionManager)
                .reader(ipoNewsFetchReader)
                .processor(ipoNewsItemConverter)
                .writer(ipoNewsWriter)
                .build();
    }

    @Bean
    public Step ipoNewsTagStep() {
        return new StepBuilder("ipoNewsTagStep", jobRepository)
                .<NewsItem, IpoNews>chunk(50, transactionManager)
                .reader(ipoNewsTagReader)
                .processor(ipoNewsItemConverter)
                .writer(ipoNewsWriter)
                .build();
    }

    @Bean
    public Step ipoNewsKoSummaryStep() {
        return new StepBuilder("ipoNewsKoSummaryStep", jobRepository)
                .<IpoNews, IpoNews>chunk(1, transactionManager)
                .reader(ipoNewsKoSummaryReader)
                .processor(ipoNewsKoSummaryProcessor)
                .writer(ipoNewsKoSummaryWriter)
                .build();
    }
}
