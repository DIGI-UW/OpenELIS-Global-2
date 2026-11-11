package org.openelisglobal.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.scheduler.valueholder.CronScheduler;
import org.openelisglobal.security.DaemonAuthenticationToken;
import org.openelisglobal.security.DaemonContextExecutor;
import org.openelisglobal.spring.util.SpringContext;
import org.quartz.Scheduler;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Regression test for the startup crash where {@code addOrRunSchedule}'s
 * catch-up dispatch invoked the {@code @Async} {@code ImmediateJobRunner} from
 * the unauthenticated bootstrap thread: the async task decorator rejects
 * submissions with no authenticated SecurityContext, and the resulting
 * IllegalStateException escaped context refresh, so the app failed to boot on
 * any install whose reporting cron time had already passed. The fix dispatches
 * under the daemon context — this test pins that contract: the runner must be
 * invoked with a {@link DaemonAuthenticationToken} in the SecurityContext, and
 * the bootstrap thread's (empty) context must be restored afterward.
 */
public class SchedulerConfigDaemonDispatchTest {

    private static final String DAEMON_ID = "7";

    private AutowireCapableBeanFactory previousSpringFactory;
    private SchedulerConfig schedulerConfig;
    private IImmediateJobRunner runner;
    private Scheduler quartzScheduler;

    @Before
    public void setUp() throws Exception {
        SecurityContextHolder.clearContext();

        runner = mock(IImmediateJobRunner.class);
        quartzScheduler = mock(Scheduler.class);

        AutowireCapableBeanFactory mockFactory = mock(AutowireCapableBeanFactory.class);
        when(mockFactory.getBean(IImmediateJobRunner.class)).thenReturn(runner);
        // DateUtil's static init resolves DefaultConfigurationProperties via
        // SpringContext; serve the production default ("X") so class
        // initialization behaves identically whether this test or an
        // integration test loads DateUtil first in the shared JVM.
        DefaultConfigurationProperties config = mock(DefaultConfigurationProperties.class);
        when(config.getPropertyValue(ConfigurationProperties.Property.AmbiguousDateHolder)).thenReturn("X");
        when(mockFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(config);
        previousSpringFactory = swapSpringFactory(mockFactory);

        DaemonContextExecutor executor = new DaemonContextExecutor();
        ReflectionTestUtils.setField(executor, "daemonSysUserId", DAEMON_ID);

        schedulerConfig = new SchedulerConfig();
        ReflectionTestUtils.setField(schedulerConfig, "daemonContextExecutor", executor);
    }

    @After
    public void tearDown() throws Exception {
        swapSpringFactory(previousSpringFactory);
        SecurityContextHolder.clearContext();
    }

    @Test
    public void addOrRunSchedule_overdueCron_dispatchesRunNowUnderDaemonContext() throws Exception {
        LocalTime now = LocalTime.now();
        // The overdue branch compares hour/minute; within the first two minutes
        // of the day there is no same-day time strictly in the past to encode.
        Assume.assumeTrue("cannot encode an overdue same-day cron right after midnight",
                now.getHour() > 0 || now.getMinute() >= 2);
        LocalTime past = now.minusMinutes(2);

        AtomicReference<Authentication> authDuringDispatch = new AtomicReference<>();
        doAnswer(invocation -> {
            authDuringDispatch.set(SecurityContextHolder.getContext().getAuthentication());
            return null;
        }).when(runner).runNow(any(), any());

        schedulerConfig.addOrRunSchedule(quartzScheduler, cronScheduler("sendSiteIndicators", cronAt(past)));

        Authentication captured = authDuringDispatch.get();
        assertTrue("runNow must be dispatched under a daemon SecurityContext, got: " + captured,
                captured instanceof DaemonAuthenticationToken);
        assertEquals(DAEMON_ID, ((DaemonAuthenticationToken) captured).getDaemonSysUserId());
        assertNull("bootstrap thread's empty context must be restored after dispatch",
                SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void addOrRunSchedule_futureCron_doesNotDispatchRunNow() throws Exception {
        LocalTime now = LocalTime.now();
        Assume.assumeTrue("cannot encode a same-day future cron right before midnight",
                now.getHour() < 23 || now.getMinute() <= 57);
        LocalTime future = now.plusMinutes(2);

        schedulerConfig.addOrRunSchedule(quartzScheduler, cronScheduler("sendSiteIndicators", cronAt(future)));

        verify(runner, never()).runNow(any(), any());
    }

    private static String cronAt(LocalTime time) {
        return "0 " + time.getMinute() + " " + time.getHour() + " * * ?";
    }

    private static CronScheduler cronScheduler(String jobName, String cronStatement) {
        CronScheduler cronScheduler = new CronScheduler();
        cronScheduler.setJobName(jobName);
        cronScheduler.setCronStatement(cronStatement);
        cronScheduler.setActive(true);
        return cronScheduler;
    }

    private static AutowireCapableBeanFactory swapSpringFactory(AutowireCapableBeanFactory replacement)
            throws Exception {
        Field factoryField = SpringContext.class.getDeclaredField("factory");
        factoryField.setAccessible(true);
        AutowireCapableBeanFactory previous = (AutowireCapableBeanFactory) factoryField.get(null);
        factoryField.set(null, replacement);
        return previous;
    }
}
