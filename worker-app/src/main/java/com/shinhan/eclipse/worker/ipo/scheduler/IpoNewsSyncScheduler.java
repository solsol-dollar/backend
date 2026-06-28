package com.shinhan.eclipse.worker.ipo.scheduler;

import com.shinhan.eclipse.worker.ipo.repository.IpoNewsIpoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
public class IpoNewsSyncScheduler {

    private final JobLauncher jobLauncher;
    private final Job ipoNewsJob;
    private final IpoNewsIpoRepository ipoRepository;
    private final TransactionTemplate transactionTemplate;

    public IpoNewsSyncScheduler(
            JobLauncher jobLauncher,
            @Qualifier("ipoNewsFetchOnlyJob") Job ipoNewsFetchOnlyJob,
            IpoNewsIpoRepository ipoRepository,
            TransactionTemplate transactionTemplate
    ) {
        this.jobLauncher = jobLauncher;
        this.ipoNewsJob = ipoNewsFetchOnlyJob;
        this.ipoRepository = ipoRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(cron = "0 59 19 * * *")
    public void triggerIpoNewsSync() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("runAt", System.currentTimeMillis())
                    .toJobParameters();
            log.info("IPO 뉴스 수집 잡 시작: {}", ipoNewsJob.getName());
            jobLauncher.run(ipoNewsJob, params);
            log.info("IPO 뉴스 수집 잡 완료: {}", ipoNewsJob.getName());

            Integer count = transactionTemplate.execute(status ->
                    ipoRepository.deactivateIposWithInsufficientNews());
            log.info("뉴스 부족 IPO INACTIVE 처리: {}건", count);
        } catch (Exception e) {
            log.error("IPO 뉴스 수집 잡 실행 실패", e);
            
        }
    }
}
