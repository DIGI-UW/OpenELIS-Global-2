package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * With a dashed accession format such as YEARNUM_DASH_SEVEN (26-0000001) the
 * patient report must keep the whole accession number: the dash belongs to the
 * number, only a trailing "-&lt;sample item number&gt;" is not part of it.
 */
public class PatientReportDashedAccessionTest {

    private final Sample sample = new Sample();
    private Object previousContext;
    private Object previousFactory;
    private Object previousMessageUtil;

    @Before
    public void setUp() {
        previousContext = ReflectionTestUtils.getField(SpringContext.class, "context");
        previousFactory = ReflectionTestUtils.getField(SpringContext.class, "factory");
        previousMessageUtil = ReflectionTestUtils.getField(MessageUtil.class, "instance");
        MessageUtil.setMessageSource(new StaticMessageSource());
        // Every bean the reports look up through SpringContext is a plain mock; the
        // configured accession format is therefore not ALPHANUM.
        AutowireCapableBeanFactory beans = mock(AutowireCapableBeanFactory.class, invocation -> {
            Object type = invocation.getArguments().length == 1 ? invocation.getArgument(0) : null;
            if (!"getBean".equals(invocation.getMethod().getName()) || !(type instanceof Class)) {
                return null;
            }
            return mock((Class<?>) type);
        });
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getAutowireCapableBeanFactory()).thenReturn(beans);
        new SpringContext().setApplicationContext(context);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(SpringContext.class, "context", previousContext);
        ReflectionTestUtils.setField(SpringContext.class, "factory", previousFactory);
        ReflectionTestUtils.setField(MessageUtil.class, "instance", previousMessageUtil);
    }

    private <T extends PatientReport> T withAccession(T report, String accessionNumber) {
        SampleService sampleService = (SampleService) ReflectionTestUtils.getField(report, "sampleService");
        when(sampleService.getAccessionNumber(sample)).thenReturn(accessionNumber);
        return report;
    }

    @Test
    public void completionMapKeyIsTheWholeAccessionNumber() {
        PatientReport report = withAccession(new PatientCILNSPClinical(), "26-0000001");

        assertEquals("26-0000001", report.convertToAlphaNumericDisplay(sample));
    }

    @Test
    public void clinicalReportShowsTheWholeAccessionNumber() {
        PatientClinicalReport report = withAccession(new PatientClinicalReport(), "26-0000001");
        ClinicalPatientData item = new ClinicalPatientData();
        item.setAccessionNumber("26-0000001-1");
        List<ClinicalPatientData> items = new ArrayList<>(List.of(item));
        Map<String, Boolean> complete = new HashMap<>();
        complete.put(report.convertToAlphaNumericDisplay(sample), Boolean.TRUE);
        ReflectionTestUtils.setField(report, "reportItems", items);
        ReflectionTestUtils.setField(report, "sampleCompleteMap", complete);
        ReflectionTestUtils.setField(report, "sampleCorrectedMap", new HashMap<String, Boolean>());

        ReflectionTestUtils.invokeMethod(report, "buildReport");

        assertEquals("26-0000001", item.getAccessionNumber());
    }

    @Test
    public void accessionWithoutADashIsUnchanged() {
        PatientReport report = withAccession(new PatientCILNSPClinical(), "DEV01260000000001");

        assertEquals("DEV01260000000001", report.convertToAlphaNumericDisplay(sample));
    }
}
