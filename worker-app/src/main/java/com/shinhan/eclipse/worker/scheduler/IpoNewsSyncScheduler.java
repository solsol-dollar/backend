package com.shinhan.eclipse.worker.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IpoNewsSyncScheduler {

    private final JobLauncher jobLauncher;
    private final Job ipoNewsSyncJob;

    @Scheduled(cron = "0 0 */6 * * *")  // 6시간마다
    public void triggerIpoNewsSync() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("runAt", System.currentTimeMillis())  // 매 실행마다 새 파라미터
                    .toJobParameters();
            jobLauncher.run(ipoNewsSyncJob, params);
            log.info("IPO news sync job launched");
        } catch (Exception e) {
            log.error("Failed to launch IPO news sync job", e);
        }
    }
}
