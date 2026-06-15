package com.shinhan.eclipse.worker.job;

import com.shinhan.eclipse.domain.ipo.IpoNews;
import com.shinhan.eclipse.worker.dto.NewsItem;
import com.shinhan.eclipse.worker.processor.IpoNewsAiSummaryProcessor;
import com.shinhan.eclipse.worker.reader.IpoNewsFetchReader;
import com.shinhan.eclipse.worker.writer.IpoNewsWriter;
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
    private final IpoNewsAiSummaryProcessor ipoNewsAiSummaryProcessor;
    private final IpoNewsWriter ipoNewsWriter;

    @Bean
    public Job ipoNewsSyncJob() {
        return new JobBuilder("ipoNewsSyncJob", jobRepository)
                .start(ipoNewsSyncStep())
                .build();
    }

    @Bean
    public Step ipoNewsSyncStep() {
        return new StepBuilder("ipoNewsSyncStep", jobRepository)
                .<NewsItem, IpoNews>chunk(5, transactionManager)
                .reader(ipoNewsFetchReader)
                .processor(ipoNewsAiSummaryProcessor)
                .writer(ipoNewsWriter)
                .faultTolerant()
                .skip(Exception.class)  // AI 실패 등 개별 항목 오류 시 skip
                .skipLimit(20)
                .build();
    }
}
