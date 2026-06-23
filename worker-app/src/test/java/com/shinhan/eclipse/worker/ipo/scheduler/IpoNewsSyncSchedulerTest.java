package com.shinhan.eclipse.worker.ipo.scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class IpoNewsSyncSchedulerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(JobLauncher.class, () -> mock(JobLauncher.class))
            .withUserConfiguration(IpoNewsSyncScheduler.class);

    @Test
<<<<<<< HEAD
    void usesFetchOnlyJobWhenSyncJobIsUnavailable() {
=======
    void alwaysUsesFetchOnlyJob() {
>>>>>>> 0ab9ceab761178cdcb577f93dd4fe0d4134b9e21
        Job fetchOnlyJob = job("ipoNewsFetchOnlyJob");

        contextRunner
                .withBean("ipoNewsFetchOnlyJob", Job.class, () -> fetchOnlyJob)
                .run(context -> assertSelectedJob(context.getBean(IpoNewsSyncScheduler.class), fetchOnlyJob));
    }

<<<<<<< HEAD
    @Test
    void usesSyncJobWhenSyncJobIsAvailable() {
        Job fetchOnlyJob = job("ipoNewsFetchOnlyJob");
        Job syncJob = job("ipoNewsSyncJob");

        contextRunner
                .withBean("ipoNewsFetchOnlyJob", Job.class, () -> fetchOnlyJob)
                .withBean("ipoNewsSyncJob", Job.class, () -> syncJob)
                .run(context -> assertSelectedJob(context.getBean(IpoNewsSyncScheduler.class), syncJob));
    }

=======
>>>>>>> 0ab9ceab761178cdcb577f93dd4fe0d4134b9e21
    private static Job job(String name) {
        Job job = mock(Job.class);
        given(job.getName()).willReturn(name);
        return job;
    }

    private static void assertSelectedJob(IpoNewsSyncScheduler scheduler, Job expectedJob) {
        assertThat(ReflectionTestUtils.getField(scheduler, "ipoNewsJob")).isSameAs(expectedJob);
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> 0ab9ceab761178cdcb577f93dd4fe0d4134b9e21
