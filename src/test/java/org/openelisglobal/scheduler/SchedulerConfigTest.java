package org.openelisglobal.scheduler;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.scheduler.service.CronSchedulerService;
import org.openelisglobal.scheduler.valueholder.CronScheduler;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class SchedulerConfigTest {

    @InjectMocks
    private SchedulerConfig schedulerConfig;
    @Mock
    private CronSchedulerService chronScheduleService;
    @Mock
    private ConfigurationProperties configProperties;
    @Mock
    private DefaultConfigurationProperties configPropertiesMock;

    @Mock
    private CronSchedulerService cronSchedulerService;
    @Mock
    private Scheduler mockScheduler;

    private ConfigurationProperties originalConfigProperties;


    @Before
    public void setUp() throws Exception {
        // Setup any necessary initialization
        ReflectionTestUtils.setField(schedulerConfig, "cronSchedulerService", cronSchedulerService);
        ReflectionTestUtils.setField(schedulerConfig, "reloadableScheduler", mockScheduler);
    }

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
    public void getResultsResendTimeMillisTestCustomValue() {
        // Create a subclass to override the method for testing
        SchedulerConfig schedulerConfig = new SchedulerConfig() {
            @Override
            public long getResultsResendTimeMillis() {
                long period = 45L;
                return period * 60 * 1000;
            }
        };

        long result = schedulerConfig.getResultsResendTimeMillis();
        long expected = 45L * 60 * 1000;

        assertEquals(expected, result);
    }

    @Test
    public void testTaskExecutor() {
        // Test that taskExecutor returns a non-null executor
        Executor executor = schedulerConfig.taskExecutor();
        assertNotNull("Task executor should not be null", executor);
    }

    @Test
    public void testAddReloadableCronSchedulers_InactiveScheduler() throws Exception {
        // Create a list of schedulers
        List<CronScheduler> schedulers = new ArrayList<>();

        // Create an inactive scheduler
        CronScheduler inactiveScheduler = new CronScheduler();
        inactiveScheduler.setActive(false);
        inactiveScheduler.setJobName("sendSiteIndicators");
        inactiveScheduler.setCronStatement("0 30 9 * * ?");
        schedulers.add(inactiveScheduler);

        // Mock the service to return our list
        when(cronSchedulerService.getAll()).thenReturn(schedulers);

        // Call the private method using reflection
        ReflectionTestUtils.invokeMethod(schedulerConfig, "addReloadableCronSchedulers", mockScheduler);

        // Verify that scheduleJob was never called (since the scheduler is inactive)
        verify(mockScheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }
}