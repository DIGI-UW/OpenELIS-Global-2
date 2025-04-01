package org.openelisglobal.scheduler;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.scheduler.service.CronSchedulerService;

@RunWith(MockitoJUnitRunner.class)
public class SchedulerConfigTest {

    @InjectMocks
    private SchedulerConfig schedulerConfig;

    @Mock
    private CronSchedulerService chronScheduleService;

    @Mock
    private ConfigurationProperties configProperties;

    @Mock
    private DefaultConfigurationProperties configPropertiesMock;

    private ConfigurationProperties originalConfigProperties;

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

}
