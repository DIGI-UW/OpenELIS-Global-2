package org.openelisglobal.sample.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.sample.form.SampleTbEntryForm;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class TbSampleServiceImplTest {

    @Mock
    private SampleService sampleService;

    @Mock
    private IStatusService statusService;

    @Mock
    private AutowireCapableBeanFactory beanFactory;

    @Mock
    private DefaultConfigurationProperties configurationProperties;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private TbSampleServiceImpl tbSampleService;

    private AutowireCapableBeanFactory previousFactory;
    private Object previousMessageUtilInstance;

    @Before
    public void setUp() {
        previousFactory = (AutowireCapableBeanFactory) ReflectionTestUtils.getField(SpringContext.class, "factory");
        previousMessageUtilInstance = ReflectionTestUtils.getField(MessageUtil.class, "instance");
        ReflectionTestUtils.setField(SpringContext.class, "factory", beanFactory);

        when(beanFactory.getBean(IStatusService.class)).thenReturn(statusService);
        when(beanFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(configurationProperties);
        when(statusService.getStatusID(OrderStatus.Entered)).thenReturn("1");
        when(configurationProperties.getPropertyValue(any(ConfigurationProperties.Property.class))).thenReturn("X");
        Mockito.lenient().when(configurationProperties.getPropertyValue(anyString())).thenReturn("human");

        when(messageSource.getMessage(anyString(), any(), anyString(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if ("date.format".equals(key) || "date.format.formatKey".equals(key)) {
                return "yyyy-MM-dd";
            }
            if ("dateTime.format".equals(key) || "timestamp.format.formatKey".equals(key)) {
                return "yyyy-MM-dd HH:mm";
            }
            if ("time.format".equals(key) || "time.format.formatKey".equals(key)) {
                return "HH:mm";
            }
            return invocation.getArgument(2);
        });
        Mockito.lenient().when(messageSource.getMessage(anyString(), any(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if ("date.format".equals(key) || "date.format.formatKey".equals(key)) {
                return "yyyy-MM-dd";
            }
            if ("dateTime.format".equals(key) || "timestamp.format.formatKey".equals(key)) {
                return "yyyy-MM-dd HH:mm";
            }
            return key;
        });

        MessageUtil.setMessageSource(messageSource);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(SpringContext.class, "factory", previousFactory);
        ReflectionTestUtils.setField(MessageUtil.class, "instance", previousMessageUtilInstance);
    }

    @Test
    public void persistSampleData_whenSampleIdProvided_fetchesManagedSampleAndUpdates() {
        Sample existingSample = new Sample();
        existingSample.setId("42");
        existingSample.setAccessionNumber("LAB-TB-001");
        existingSample.setDomain("H");
        existingSample.setStatusId("1");

        when(sampleService.get("42")).thenReturn(existingSample);
        when(sampleService.update(any(Sample.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setSampleId("42");
        form.setLabNo("LAB-TB-001");
        form.setRequestDate("2026-10-25");
        form.setReceivedDate("2026-10-26");
        form.setSysUserId("99");

        String resultId = ReflectionTestUtils.invokeMethod(tbSampleService, "persistSampleData", form);

        assertEquals("42", resultId);
        ArgumentCaptor<Sample> captor = ArgumentCaptor.forClass(Sample.class);
        verify(sampleService).update(captor.capture());
        Sample updated = captor.getValue();
        assertEquals("42", updated.getId());
        assertEquals("LAB-TB-001", updated.getAccessionNumber());
        assertEquals("99", updated.getSysUserId());
        assertEquals("2026-10-25", updated.getCollectionDateForDisplay());
        assertEquals("2026-10-26", updated.getReceivedDateForDisplay());
        assertEquals(OrderPriority.ROUTINE, updated.getPriority());
    }

    @Test(expected = LIMSRuntimeException.class)
    public void persistSampleData_whenSampleIdProvided_sampleNotFound_throwsException() {
        when(sampleService.get("999")).thenReturn(null);

        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setSampleId("999");

        ReflectionTestUtils.invokeMethod(tbSampleService, "persistSampleData", form);
    }

    @Test
    public void persistSampleData_whenSampleIdEmpty_createsAndInsertsNewSample() {
        when(sampleService.insert(any(Sample.class))).thenReturn("101");

        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setSampleId(null);
        form.setLabNo("LAB-NEW-001");
        form.setRequestDate("2026-10-25");
        form.setReceivedDate("2026-10-26");
        form.setSysUserId("5");

        String resultId = ReflectionTestUtils.invokeMethod(tbSampleService, "persistSampleData", form);

        assertEquals("101", resultId);
        ArgumentCaptor<Sample> captor = ArgumentCaptor.forClass(Sample.class);
        verify(sampleService).insert(captor.capture());
        Sample inserted = captor.getValue();
        assertEquals("LAB-NEW-001", inserted.getAccessionNumber());
        assertEquals("H", inserted.getDomain());
        assertEquals("1", inserted.getStatusId());
        assertEquals("5", inserted.getSysUserId());
        assertEquals("2026-10-25", inserted.getCollectionDateForDisplay());
        assertEquals("2026-10-26", inserted.getReceivedDateForDisplay());
    }
}
