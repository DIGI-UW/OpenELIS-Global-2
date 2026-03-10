package org.openelisglobal.genericsample.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.barcode.service.BarcodeInfoService;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.genericsample.form.GenericSampleOrderForm;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.sample.dao.SampleDAO;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class GenericSampleOrderServiceImplTest {

    @Mock
    private SampleService sampleService;

    @Mock
    private SampleItemService sampleItemService;

    @Mock
    private SampleDAO sampleDAO;

    @Mock
    private BarcodeInfoService barcodeInfoService;

    @Mock
    private IStatusService statusService;

    @Mock
    private AutowireCapableBeanFactory beanFactory;

    @Mock
    private IAccessionNumberGenerator accessionNumberGenerator;

    @Mock
    private DefaultConfigurationProperties configurationProperties;

    @Mock
    private MessageSource messageSource;

    private GenericSampleOrderServiceImpl service;

    @Before
    public void setUp() {
        service = new GenericSampleOrderServiceImpl();
        ReflectionTestUtils.setField(service, "sampleService", sampleService);
        ReflectionTestUtils.setField(service, "sampleItemService", sampleItemService);
        ReflectionTestUtils.setField(service, "sampleDAO", sampleDAO);
        ReflectionTestUtils.setField(service, "barcodeInfoService", barcodeInfoService);
        ReflectionTestUtils.setField(service, "accessionNumberGenerator", accessionNumberGenerator);
        ReflectionTestUtils.setField(SpringContext.class, "factory", beanFactory);

        when(beanFactory.getBean(IStatusService.class)).thenReturn(statusService);
        when(beanFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(configurationProperties);
        when(statusService.getStatusID(OrderStatus.Entered)).thenReturn("2");
        when(configurationProperties.getPropertyValue(any(ConfigurationProperties.Property.class))).thenReturn("X");
        when(configurationProperties.getPropertyValue(anyString())).thenReturn("human");
        when(messageSource.getMessage(anyString(), any(), anyString(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if ("date.format".equals(key) || "date.format.formatKey".equals(key)) {
                return "MM/dd/yyyy";
            }
            if ("dateTime.format".equals(key) || "timestamp.format.formatKey".equals(key)) {
                return "MM/dd/yyyy HH:mm";
            }
            if ("time.format".equals(key) || "time.format.formatKey".equals(key)) {
                return "HH:mm";
            }
            if ("timestamp.format.formatKey.12".equals(key)) {
                return "MM/dd/yyyy hh:mm a";
            }
            return invocation.getArgument(2);
        });
        MessageUtil.setMessageSource(messageSource);

        doAnswer(invocation -> {
            Sample sample = invocation.getArgument(0);
            if (sample.getId() == null) {
                sample.setId("sample-1");
            }
            return null;
        }).when(sampleService).insertDataWithAccessionNumber(any(Sample.class));
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(SpringContext.class, "factory", null);
    }

    @Test
    public void saveGenericSampleOrderInternal_defaultsLabelCountsToOneWhenMissing() throws Exception {
        GenericSampleOrderForm form = buildForm("M2-DEFAULT-001", null, null);

        Sample saved = new Sample();
        saved.setId("sample-1");
        saved.setAccessionNumber("M2-DEFAULT-001");
        when(sampleService.get("sample-1")).thenReturn(saved);

        Map<String, Object> result = service.saveGenericSampleOrderInternal(form, "1001");

        assertTrue("Save should report success", (Boolean) result.get("success"));
        verify(barcodeInfoService).saveBarcodeInfoForSampleAndSampleItems(saved, 1, 1);
    }

    @Test
    public void saveGenericSampleOrderInternal_usesExplicitLabelCountsWhenProvided() throws Exception {
        GenericSampleOrderForm form = buildForm("M2-EXPLICIT-001", 3, 4);

        Sample saved = new Sample();
        saved.setId("sample-1");
        saved.setAccessionNumber("M2-EXPLICIT-001");
        when(sampleService.get("sample-1")).thenReturn(saved);

        Map<String, Object> result = service.saveGenericSampleOrderInternal(form, "1001");

        assertTrue("Save should report success", (Boolean) result.get("success"));
        verify(barcodeInfoService).saveBarcodeInfoForSampleAndSampleItems(saved, 3, 4);
    }

    @Test
    public void saveGenericSampleOrderInternal_handlesMissingDefaultFieldsByApplyingFallbacks() throws Exception {
        GenericSampleOrderForm form = new GenericSampleOrderForm();
        when(accessionNumberGenerator.getNextAccessionNumber(eq(null), eq(true))).thenReturn("AUTO-0001");
        when(sampleService.getSampleByAccessionNumber("AUTO-0001")).thenReturn(null);

        Sample saved = new Sample();
        saved.setId("sample-1");
        saved.setAccessionNumber("AUTO-0001");
        when(sampleService.get("sample-1")).thenReturn(saved);

        Map<String, Object> result = service.saveGenericSampleOrderInternal(form, "1001");

        assertTrue("Save should report success when default fields are missing", (Boolean) result.get("success"));
        assertNotNull("Generated accession number should be returned", result.get("accessionNumber"));
        verify(barcodeInfoService).saveBarcodeInfoForSampleAndSampleItems(saved, 1, 1);
    }

    private GenericSampleOrderForm buildForm(String labNo, Integer numOrderLabels, Integer numSpecimenLabels) {
        GenericSampleOrderForm form = new GenericSampleOrderForm();
        GenericSampleOrderForm.DefaultFields defaultFields = new GenericSampleOrderForm.DefaultFields();
        defaultFields.setLabNo(labNo);
        defaultFields.setNumOrderLabels(numOrderLabels);
        defaultFields.setNumSpecimenLabels(numSpecimenLabels);
        form.setDefaultFields(defaultFields);
        return form;
    }
}
