package org.openelisglobal.compliance.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStatus;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.ComplianceThresholdValueMap;
import org.openelisglobal.compliance.valueholder.ThresholdType;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
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
    @Mock
    private DictionaryService dictionaryService;
    @Mock
    private IStatusService statusService;

    @InjectMocks
    private ComplianceEvaluationServiceImpl service;

    private static final String FINALIZED = "6";

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
        analysis.setStatusId(FINALIZED);
        lenient().when(statusService.matches(anyString(), eq(AnalysisStatus.Finalized)))
                .thenAnswer(invocation -> FINALIZED.equals(invocation.getArgument(0)));
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

    private ComplianceThreshold threshold(ThresholdType type, String min, String max) {
        ComplianceThreshold t = new ComplianceThreshold();
        t.setParameterCode("Free chlorine");
        t.setDisplayName("Free chlorine");
        t.setThresholdType(type);
        t.setMinValue(min == null ? null : new BigDecimal(min));
        t.setMaxValue(max == null ? null : new BigDecimal(max));
        return t;
    }

    private void givenOneTest(List<ComplianceThreshold> thresholds, String value) {
        when(sampleComplianceStandardDAO.getAllForSample("1")).thenReturn(List.of(link));
        when(sampleItemService.getSampleItemsBySampleId("1")).thenReturn(List.of(sampleItem));
        when(analysisService.getMaxRevisionAnalysesBySample(sampleItem)).thenReturn(List.of(analysis));
        when(complianceThresholdService.getThresholdsByTestAndStandard("5", "10")).thenReturn(thresholds);
        when(resultService.getResultsByAnalysis(analysis)).thenReturn(List.of(resultWithValue(value)));
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
    public void rangeThreshold_valueJustOutside_isNonCompliant() {
        givenOneTest(List.of(rangeThreshold("4.5", "8.5")), "8.8");

        assertEquals("no implicit margin outside a limit", ComplianceStatus.NON_COMPLIANT,
                service.evaluate(sample).getOverallStatus());
    }

    @Test
    public void rangeThreshold_valueBelowMinimum_isNonCompliant() {
        ComplianceThreshold chlorine = rangeThreshold("0.2", "5");
        chlorine.setUnits("mg/L");
        givenOneTest(List.of(chlorine), "0.1");

        ComplianceEvaluationResult result = service.evaluate(sample);

        assertEquals(ComplianceStatus.NON_COMPLIANT, result.getOverallStatus());
        assertEquals("0.2 - 5 mg/L", result.getParameterResults().get(0).getThresholdDisplay());
    }

    @Test
    public void everyLimitOfTheTestIsApplied() {
        givenOneTest(
                List.of(threshold(ThresholdType.MINIMUM, "0.2", null), threshold(ThresholdType.MAXIMUM, null, "5")),
                "7.0");

        ComplianceEvaluationResult result = service.evaluate(sample);

        assertEquals("7.0 meets the minimum but not the maximum", ComplianceStatus.NON_COMPLIANT,
                result.getOverallStatus());
        assertEquals(1, result.getParameterResults().size());
        assertEquals("≥ 0.2; ≤ 5", result.getParameterResults().get(0).getThresholdDisplay());
    }

    @Test
    public void aConfiguredAdvisoryZoneMakesAMissBorderline() {
        List<ComplianceThreshold> withZone = List.of(rangeThreshold("4.5", "8.5"),
                threshold(ThresholdType.BORDERLINE, "8.5", "9.0"));
        givenOneTest(withZone, "8.8");
        assertEquals(ComplianceStatus.BORDERLINE, service.evaluate(sample).getOverallStatus());

        givenOneTest(withZone, "9.5");
        assertEquals(ComplianceStatus.NON_COMPLIANT, service.evaluate(sample).getOverallStatus());

        givenOneTest(withZone, "7.0");
        assertEquals("inside the limits stays compliant", ComplianceStatus.COMPLIANT,
                service.evaluate(sample).getOverallStatus());
    }

    @Test
    public void aDeactivatedThresholdIsNotApplied() {
        ComplianceThreshold retired = threshold(ThresholdType.MAXIMUM, null, "1");
        retired.setIsActive(false);
        givenOneTest(List.of(rangeThreshold("0.2", "5"), retired), "3");

        assertEquals(ComplianceStatus.COMPLIANT, service.evaluate(sample).getOverallStatus());
    }

    @Test
    public void everyLinkedStandardIsEvaluated() {
        ComplianceStandard second = new ComplianceStandard();
        second.setId("11");
        second.setRegulationNumber("PP-2");
        standard.setRegulationNumber("PP-1");
        SampleComplianceStandard secondLink = new SampleComplianceStandard();
        secondLink.setSample(sample);
        secondLink.setComplianceStandard(second);
        when(sampleComplianceStandardDAO.getAllForSample("1")).thenReturn(List.of(link, secondLink));
        when(sampleItemService.getSampleItemsBySampleId("1")).thenReturn(List.of(sampleItem));
        when(analysisService.getMaxRevisionAnalysesBySample(sampleItem)).thenReturn(List.of(analysis));
        when(complianceThresholdService.getThresholdsByTestAndStandard("5", "10"))
                .thenReturn(List.of(rangeThreshold("0.2", "5")));
        when(complianceThresholdService.getThresholdsByTestAndStandard("5", "11"))
                .thenReturn(List.of(threshold(ThresholdType.MAXIMUM, null, "2")));
        when(resultService.getResultsByAnalysis(analysis)).thenReturn(List.of(resultWithValue("3")));

        ComplianceEvaluationResult result = service.evaluate(sample);

        assertEquals("compliant with PP-1, not with PP-2", ComplianceStatus.NON_COMPLIANT, result.getOverallStatus());
        assertEquals(List.of("PP-1: 0.2 - 5", "PP-2: ≤ 2"), result.getParameterResults().stream()
                .map(ComplianceEvaluationResult.ParameterResult::getThresholdDisplay).toList());
    }

    @Test
    public void aSelectListResultTakesItsOptionsMappedStatus() {
        ComplianceThreshold selectMap = threshold(ThresholdType.SELECT_MAP, null, null);
        ComplianceThresholdValueMap absent = new ComplianceThresholdValueMap();
        absent.setOptionValue("Absent");
        absent.setComplianceStatus(ComplianceStatus.COMPLIANT);
        ComplianceThresholdValueMap present = new ComplianceThresholdValueMap();
        present.setOptionValue("Present");
        present.setComplianceStatus(ComplianceStatus.NON_COMPLIANT);
        selectMap.setValueMappings(List.of(absent, present));
        Dictionary absentEntry = new Dictionary();
        absentEntry.setDictEntry("Absent");
        Dictionary presentEntry = new Dictionary();
        presentEntry.setDictEntry("Present");
        Dictionary unmappedEntry = new Dictionary();
        unmappedEntry.setDictEntry("Not tested");
        when(dictionaryService.getDictionaryById("301")).thenReturn(absentEntry);
        when(dictionaryService.getDictionaryById("302")).thenReturn(presentEntry);
        when(dictionaryService.getDictionaryById("303")).thenReturn(unmappedEntry);

        givenOneTest(List.of(selectMap), "301");
        assertEquals(ComplianceStatus.COMPLIANT, service.evaluate(sample).getOverallStatus());
        givenOneTest(List.of(selectMap), "302");
        assertEquals(ComplianceStatus.NON_COMPLIANT, service.evaluate(sample).getOverallStatus());
        givenOneTest(List.of(selectMap), "303");
        assertEquals("an unmapped option is left for review", ComplianceStatus.BORDERLINE,
                service.evaluate(sample).getOverallStatus());
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
        analysis2.setStatusId(FINALIZED);

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
                .thenReturn(List.of(rangeThreshold("4.5", "8.5"), threshold(ThresholdType.BORDERLINE, "8.5", "9.0")));
        when(resultService.getResultsByAnalysis(analysis)).thenReturn(List.of(resultWithValue("8.8")));

        ComplianceEvaluationResult result = service.evaluate(sample);

        assertNotNull(result);
        assertEquals(ComplianceStatus.BORDERLINE, result.getOverallStatus());
    }

    @Test
    public void aResultNotYetValidatedIsNotGraded() {
        analysis.setStatusId("15");
        when(sampleComplianceStandardDAO.getAllForSample("1")).thenReturn(List.of(link));
        when(sampleItemService.getSampleItemsBySampleId("1")).thenReturn(List.of(sampleItem));
        when(analysisService.getMaxRevisionAnalysesBySample(sampleItem)).thenReturn(List.of(analysis));
        lenient().when(complianceThresholdService.getThresholdsByTestAndStandard("5", "10"))
                .thenReturn(List.of(threshold(ThresholdType.RANGE, "0.2", "5")));
        lenient().when(resultService.getResultsByAnalysis(analysis)).thenReturn(List.of(resultWithValue("5.3")));

        assertNull("an edited result waiting for validation is not on the report", service.evaluate(sample));
        verify(resultService, never()).getResultsByAnalysis(analysis);
    }
}
