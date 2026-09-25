package org.openelisglobal.coldstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.coldstorage.service.FreezerReadingService;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.service.ModbusClientService;
import org.openelisglobal.coldstorage.service.ReadingIngestionService;
import org.openelisglobal.coldstorage.service.SystemConfigService;
import org.openelisglobal.coldstorage.service.impl.ModbusPollingService;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTask;

/**
 * The test context runs no scheduler, so a fresh {@code @Scheduled} processor
 * registers the task for inspection.
 */
public class ModbusPollingScheduleTest extends BaseWebContextSensitiveTest {

    @Test
    public void pollDevices_shouldBeScheduledAtAFixedRateOfTheConfiguredPollInterval() {
        ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) webApplicationContext)
                .getBeanFactory();
        ScheduledAnnotationBeanPostProcessor processor = new ScheduledAnnotationBeanPostProcessor();
        processor.setBeanFactory(beanFactory);
        processor.setEmbeddedValueResolver(new EmbeddedValueResolver(beanFactory));

        ModbusPollingService service = new ModbusPollingService(mock(SystemConfigService.class),
                mock(FreezerService.class), mock(ModbusClientService.class), mock(ReadingIngestionService.class),
                mock(FreezerReadingService.class), mock(ExecutorService.class));
        processor.postProcessAfterInitialization(service, "modbusPollingService");

        List<IntervalTask> pollTasks = processor.getScheduledTasks().stream().map(ScheduledTask::getTask)
                .filter(task -> task.toString().endsWith(".pollDevices")).filter(IntervalTask.class::isInstance)
                .map(IntervalTask.class::cast).toList();
        String configured = webApplicationContext.getEnvironment()
                .getProperty("org.openelisglobal.freezermonitoring.modbus.poll-interval", "PT5M");

        assertEquals("pollDevices should be registered once: " + pollTasks, 1, pollTasks.size());
        assertTrue("pollDevices must run at a fixed rate, not a fixed delay: " + pollTasks.get(0).getClass(),
                pollTasks.get(0) instanceof FixedRateTask);
        assertEquals(Duration.parse(configured), pollTasks.get(0).getIntervalDuration());
        processor.destroy();
    }
}
