package org.openelisglobal.scheduler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openelisglobal.scheduler.service.CronSchedulerService;
import org.openelisglobal.scheduler.valueholder.CronScheduler;
import org.openelisglobal.spring.util.SpringContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class SchedulerConfigTest {

    @InjectMocks
    private SchedulerConfig schedulerConfig;

    @Mock
    private CronSchedulerService cronSchedulerService;

    @Mock
    private Scheduler mockScheduler;

    @Mock
    private ScheduledTaskRegistrar taskRegistrar;

    @Test
    public void testTaskExecutor() {
        // When
        Executor executor = schedulerConfig.taskExecutor();
        // Then
        assertNotNull(executor, "Task executor should not be null");
    }

    @Test
    public void testReloadSchedulesWithEmptyList() throws SchedulerException {
        // Given
        List<CronScheduler> schedulers = new ArrayList<>();
        when(cronSchedulerService.getAll()).thenReturn(schedulers);

        // When
        schedulerConfig.reloadSchedules();

        // Then
        verify(mockScheduler).shutdown();
        verify(mockScheduler, never()).scheduleJob(any(), any());
    }

    @Test
    public void testReloadSchedulesWithCronStatementNever() throws SchedulerException {
        // Given: a cron scheduler with cron statement "never" should not trigger job scheduling.
        List<CronScheduler> schedulers = new ArrayList<>();
        CronScheduler neverScheduler = new CronScheduler();
        neverScheduler.setActive(true);
        neverScheduler.setJobName("sendSiteIndicators");
        neverScheduler.setCronStatement("never");
        schedulers.add(neverScheduler);

        when(cronSchedulerService.getAll()).thenReturn(schedulers);

        // When: reloadSchedules is called.
        schedulerConfig.reloadSchedules();

        // Then: verify that the scheduler was shut down,
        // and no job was scheduled since the cron statement is "never".
        verify(mockScheduler).shutdown();
        verify(mockScheduler, never()).scheduleJob(any(), any());
    }

}