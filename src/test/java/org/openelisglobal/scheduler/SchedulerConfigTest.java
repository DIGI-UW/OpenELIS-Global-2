package org.openelisglobal.scheduler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openelisglobal.scheduler.service.CronSchedulerService;
import org.openelisglobal.scheduler.valueholder.CronScheduler;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

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
    public void getResultsResendTimeMillisTestDefaultValue() {
        long default_period_millis = 30L * 1000 * 60;

        SchedulerConfig schedulerConfig = new SchedulerConfig() {
            @Override
            public long getResultsResendTimeMillis() {
                long period = 30L;
                return period * 60 * 1000;
            }
        };

        long result = schedulerConfig.getResultsResendTimeMillis();

        assertEquals(default_period_millis, result);
    }

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

        // When
        schedulerConfig.reloadSchedules();

        // Then
        verify(mockScheduler).shutdown();
        verify(mockScheduler, never()).scheduleJob(any(), any());
    }

}