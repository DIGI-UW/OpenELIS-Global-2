package org.openelisglobal.compliance.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStatus;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.ThresholdType;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.dao.SampleComplianceStandardDAO;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sample.valueholder.SampleComplianceStandard;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

@RunWith(MockitoJUnitRunner.class)
public class ComplianceEvaluationServiceImplTest {

    @Mock
    private SampleComplianceStandardDAO sampleComplianceStandardDAO;
    @Mock
    private ComplianceThresholdService complianceThresholdService;
    @Mock
    private SampleItemService sampleItemService;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private ResultService resultService;

    @InjectMocks
    private ComplianceEvaluationServiceImpl service;

    private Sample sample;
    private SampleItem sampleItem;
    private Analysis analysis;
    private ComplianceStandard standard;
    private SampleComplianceStandard link;

    @Before
    public void setUp() {
        sample = new Sample();
        sample.setId("1");

        standard = new ComplianceStandard();
        standard.setId("10");

        link = new SampleComplianceStandard();
        link.setSample(sample);
        link.setComplianceStandard(standard);

        sampleItem = new SampleItem();
        sampleItem.setId("100");

        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("5");

        analysis = new Analysis();
        analysis.setTest(test);
    }

    private ComplianceThreshold rangeThreshold(String min, String max) {
        ComplianceThreshold t = new ComplianceThreshold();
        t.setParameterCode("pH");
        t.setDisplayName("pH");
        t.setThresholdType(ThresholdType.RANGE);
        t.setMinValue(new BigDecimal(min));
        t.setMaxValue(new BigDecimal(max));
        t.setUnits("");
        return t;
    }

    private Result resultWithValue(String value) {
        Result r = new Result();
        r.setValue(value);
        return r;
    }

    // ---- RANGE threshold tests ----

    @Test
    public void rangeThreshold_valueInRange_isCompliant() {
        when(sampleComplianceStandardDAO.getAllForSample("1")).thenReturn(List.of(link));
        when(sampleItemService.getSampleItemsBySampleId("1")).thenReturn(List.of(sampleItem));
        when(analysisService.getMaxRevisionAnalysesBySample(sampleItem)).thenReturn(List.of(analysis));
        when(complianceThresholdService.getThresholdsByTestAndStandard("5", "10"))
                .thenReturn(List.of(rangeThreshold("4.5", "8.5")));
        when(resultService.getResultsByAnalysis(analysis)).thenReturn(List.of(resultWithValue("7.0")));

        ComplianceEvaluationResult result = service.evaluate(sample);

        assertNotNull(result);
        assertEquals(ComplianceStatus.COMPLIANT, result.getOverallStatus());
        assertEquals(ComplianceStatus.COMPLIANT, result.getParameterResults().get(0).getStatus());
    }

    @Test
    public void rangeThreshold_valueAtBoundary_isCompliant() {
        when(sampleComplianceStandardDAO.getAllForSample("1")).thenReturn(List.of(link));
        when(sampleItemService.getSampleItemsBySampleId("1")).thenReturn(List.of(sampleItem));
        when(analysisService.getMaxRevisionAnalysesBySample(sampleItem)).thenReturn(List.of(analysis));
        when(complianceThresholdService.getThresholdsByTestAndStandard("5", "10"))
                .thenReturn(List.of(rangeThreshold("4.5", "8.5")));
        when(resultService.getResultsByAnalysis(analysis)).thenReturn(List.of(resultWithValue("4.5")));

        ComplianceEvaluationResult result = service.evaluate(sample);

        assertNotNull(result);
        assertEquals(ComplianceStatus.COMPLIANT, result.getOverallStatus());
    }

    @Test
    public void rangeThreshold_valueSlightlyOutside_isBorderline() {
        // 5% outside upper boundary of 4.5–8.5 (range = 4.0; 10% margin = 0.4; ceiling = 8.9)
        when(sampleComplianceStandardDAO.getAllForSample("1")).thenReturn(List.of(link));
        when(sampleItemService.getSampleItemsBySampleId("1")).thenReturn(List.of(sampleItem));
        when(analysisService.getMaxRevisionAnalysesBySample(sampleItem)).thenReturn(List.of(analysis));
        when(complianceThresholdService.getThresholdsByTestAndStandard("5", "10"))
                .thenReturn(List.of(rangeThreshold("4.5", "8.5")));
        when(resultService.getResultsByAnalysis(analysis)).thenReturn(List.of(resultWithValue("8.8")));

        ComplianceEvaluationResult result = service.evaluate(sample);

        assertNotNull(result);
        assertEquals(ComplianceStatus.BORDERLINE, result.getOverallStatus());
    }

    @Test
    public void rangeThreshold_valueFarOutside_isNonCompliant() {
        when(sampleComplianceStandardDAO.getAllForSample("1")).thenReturn(List.of(link));
        when(sampleItemService.getSampleItemsBySampleId("1")).thenReturn(List.of(sampleItem));
        when(analysisService.getMaxRevisionAnalysesBySample(sampleItem)).thenReturn(List.of(analysis));
        when(complianceThresholdService.getThresholdsByTestAndStandard("5", "10"))
                .thenReturn(List.of(rangeThreshold("4.5", "8.5")));
        when(resultService.getResultsByAnalysis(analysis)).thenReturn(List.of(resultWithValue("12.0")));

        ComplianceEvaluationResult result = service.evaluate(sample);

        assertNotNull(result);
        assertEquals(ComplianceStatus.NON_COMPLIANT, result.getOverallStatus());
    }

    // ---- MAXIMUM threshold tests ----

    @Test
    public void maximumThreshold_valueBelowMax_isCompliant() {
        ComplianceThreshold t = new ComplianceThreshold();
        t.setParameterCode("Lead");
        t.setDisplayName("Lead (Pb)");
        t.setThresholdType(ThresholdType.MAXIMUM);
        t.setMaxValue(new BigDecimal("0.03"));
        t.setUnits("mg/L");

        when(sampleComplianceStandardDAO.getAllForSample("1")).thenReturn(List.of(link));
        when(sampleItemService.getSampleItemsBySampleId("1")).thenReturn(List.of(sampleItem));
        when(analysisService.getMaxRevisionAnalysesBySample(sampleItem)).thenReturn(List.of(analysis));
        when(complianceThresholdService.getThresholdsByTestAndStandard("5", "10")).thenReturn(List.of(t));
        when(resultService.getResultsByAnalysis(analysis)).thenReturn(List.of(resultWithValue("0.01")));

        ComplianceEvaluationResult result = service.evaluate(sample);

        assertNotNull(result);
        assertEquals(ComplianceStatus.COMPLIANT, result.getOverallStatus());
    }

    // ---- No standard linked ----

    @Test
    public void noLinkedStandard_returnsNull() {
        when(sampleComplianceStandardDAO.getAllForSample("1")).thenReturn(Collections.emptyList());

        ComplianceEvaluationResult result = service.evaluate(sample);

        assertNull(result);
    }

    // ---- No matching threshold ----

    @Test
    public void noMatchingThreshold_testExcludedFromResults() {
        when(sampleComplianceStandardDAO.getAllForSample("1")).thenReturn(List.of(link));
        when(sampleItemService.getSampleItemsBySampleId("1")).thenReturn(List.of(sampleItem));
        when(analysisService.getMaxRevisionAnalysesBySample(sampleItem)).thenReturn(List.of(analysis));
        when(complianceThresholdService.getThresholdsByTestAndStandard("5", "10"))
                .thenReturn(Collections.emptyList());

        ComplianceEvaluationResult result = service.evaluate(sample);

        assertNull(result);
    }

    // ---- Roll-up tests ----

    @Test
    public void rollUp_oneNonCompliantRestCompliant_overallNonCompliant() {
        Analysis analysis2 = new Analysis();
        org.openelisglobal.test.valueholder.Test test2 = new org.openelisglobal.test.valueholder.Test();
        test2.setId("6");
        analysis2.setTest(test2);

        ComplianceThreshold t1 = rangeThreshold("4.5", "8.5");
        t1.setParameterCode("pH");

        ComplianceThreshold t2 = new ComplianceThreshold();
        t2.setParameterCode("Lead");
        t2.setDisplayName("Lead");
        t2.setThresholdType(ThresholdType.MAXIMUM);
        t2.setMaxValue(new BigDecimal("0.03"));

        when(sampleComplianceStandardDAO.getAllForSample("1")).thenReturn(List.of(link));
        when(sampleItemService.getSampleItemsBySampleId("1")).thenReturn(List.of(sampleItem));
        when(analysisService.getMaxRevisionAnalysesBySample(sampleItem)).thenReturn(List.of(analysis, analysis2));
        when(complianceThresholdService.getThresholdsByTestAndStandard("5", "10")).thenReturn(List.of(t1));
        when(complianceThresholdService.getThresholdsByTestAndStandard("6", "10")).thenReturn(List.of(t2));
        when(resultService.getResultsByAnalysis(analysis)).thenReturn(List.of(resultWithValue("7.0")));
        when(resultService.getResultsByAnalysis(analysis2)).thenReturn(List.of(resultWithValue("0.10")));

        ComplianceEvaluationResult result = service.evaluate(sample);

        assertNotNull(result);
        assertEquals(ComplianceStatus.NON_COMPLIANT, result.getOverallStatus());
        assertEquals(2, result.getParameterResults().size());
    }

    @Test
    public void rollUp_oneBorderlineRestCompliant_overallBorderline() {
        when(sampleComplianceStandardDAO.getAllForSample("1")).thenReturn(List.of(link));
        when(sampleItemService.getSampleItemsBySampleId("1")).thenReturn(List.of(sampleItem));
        when(analysisService.getMaxRevisionAnalysesBySample(sampleItem)).thenReturn(List.of(analysis));
        when(complianceThresholdService.getThresholdsByTestAndStandard("5", "10"))
                .thenReturn(List.of(rangeThreshold("4.5", "8.5")));
        // 8.8 is within 10% of boundary of range 4.5–8.5 → BORDERLINE
        when(resultService.getResultsByAnalysis(analysis)).thenReturn(List.of(resultWithValue("8.8")));

        ComplianceEvaluationResult result = service.evaluate(sample);

        assertNotNull(result);
        assertEquals(ComplianceStatus.BORDERLINE, result.getOverallStatus());
    }
}
