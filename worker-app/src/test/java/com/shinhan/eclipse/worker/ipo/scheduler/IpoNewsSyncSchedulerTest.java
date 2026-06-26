package com.shinhan.eclipse.worker.ipo.scheduler;

import com.shinhan.eclipse.worker.ipo.repository.IpoNewsIpoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class IpoNewsSyncSchedulerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(JobLauncher.class, () -> mock(JobLauncher.class))
            .withBean(IpoNewsIpoRepository.class, () -> mock(IpoNewsIpoRepository.class))
            .withBean(TransactionTemplate.class, () -> mock(TransactionTemplate.class))
            .withUserConfiguration(IpoNewsSyncScheduler.class);

    @Test
    void alwaysUsesFetchOnlyJob() {
        Job fetchOnlyJob = job("ipoNewsFetchOnlyJob");

        contextRunner
                .withBean("ipoNewsFetchOnlyJob", Job.class, () -> fetchOnlyJob)
                .run(context -> assertSelectedJob(context.getBean(IpoNewsSyncScheduler.class), fetchOnlyJob));
    }

    private static Job job(String name) {
        Job job = mock(Job.class);
        given(job.getName()).willReturn(name);
        return job;
    }

    private static void assertSelectedJob(IpoNewsSyncScheduler scheduler, Job expectedJob) {
        assertThat(ReflectionTestUtils.getField(scheduler, "ipoNewsJob")).isSameAs(expectedJob);
    }
}
