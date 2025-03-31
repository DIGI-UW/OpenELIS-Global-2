package org.openelisglobal.scheduler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.*;
import org.mockito.Mock;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.swing.Spring;

import org.openelisglobal.scheduler.service.CronSchedulerService;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.scheduler.SchedulerConfig;
import org.openelisglobal.spring.util.SpringContext;
import java.lang.reflect.Field;




public class SchedulerConfigTest {

    @InjectMocks
    private SchedulerConfig schedulerConfig;
    
    @Mock
    private CronSchedulerService chronScheduleService;

    @Mock 
    private ConfigurationProperties configProperties;

    @Mock
    private DefaultConfigurationProperties configPropertiesMock;

    // Create a method to access ConfigurationProperties.getInstance()
    private ConfigurationProperties originalConfigProperties;
    
   @Test
   public void getResultsResendTimeMillisTestDefaultValue() {
    long default_period_millis = 30L * 1000 * 60;

    // Mock the behavior of getPropertyValue to return a valid value
    Mockito.when(configProperties.getPropertyValue(Property.resultsResendTime)).thenReturn(null);

    SchedulerConfig schedulerConfig = new SchedulerConfig();

    long result = schedulerConfig.getResultsResendTimeMillis();

    assertEquals(default_period_millis, result);
   }

}
