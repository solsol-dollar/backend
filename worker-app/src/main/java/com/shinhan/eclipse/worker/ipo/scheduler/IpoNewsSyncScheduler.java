package com.shinhan.eclipse.worker.ipo.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IpoNewsSyncScheduler {

    private final JobLauncher jobLauncher;
    private final Job ipoNewsJob;

    public IpoNewsSyncScheduler(
            JobLauncher jobLauncher,
            @Qualifier("ipoNewsFetchOnlyJob") Job ipoNewsFetchOnlyJob
    ) {
        this.jobLauncher = jobLauncher;
        this.ipoNewsJob = ipoNewsFetchOnlyJob;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void triggerIpoNewsSync() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("runAt", System.currentTimeMillis())
                    .toJobParameters();
            log.info("IPO 뉴스 수집 잡 시작: {}", ipoNewsJob.getName());
            jobLauncher.run(ipoNewsJob, params);
            log.info("IPO 뉴스 수집 잡 완료: {}", ipoNewsJob.getName());
        } catch (Exception e) {
            log.error("IPO 뉴스 수집 잡 실행 실패", e);
        }
    }
}
