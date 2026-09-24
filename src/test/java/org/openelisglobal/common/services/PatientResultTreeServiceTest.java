package org.openelisglobal.common.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.rest.provider.bean.patientHistory.PanelDisplay;
import org.openelisglobal.common.rest.provider.bean.patientHistory.ResultDisplay;
import org.openelisglobal.common.rest.provider.bean.patientHistory.ResultTree;
import org.openelisglobal.common.rest.provider.bean.patientHistory.TestDisplay;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;

/**
 * The Patient History tree has to keep apart what the unified test catalogue
 * lets a single test hold: several sample types and several result components,
 * each with its own result and its own reference range.
 */
@RunWith(MockitoJUnitRunner.class)
public class PatientResultTreeServiceTest {

    private static final String PATIENT_ID = "7";
    private static final String TEST_ID = "42";

    @Mock
    private SampleHumanService sampleHumanService;

    @Mock
    private ResultService resultService;

    @Mock
    private DictionaryService dictionaryService;

    @Mock
    private TestService testService;

    @Mock
    private PatientService patientService;

    @Mock
    private ResultLimitService resultLimitService;

    @Mock
    private TestResultComponentService testResultComponentService;

    @Mock
    private UnitOfMeasureService unitOfMeasureService;

    @InjectMocks
    private PatientResultTreeService patientResultTreeService;

    private Patient patient;
    private TestSection testSection;
    private org.openelisglobal.test.valueholder.Test bloodPressure;

    @Before
    public void setUp() {
        // The patient carries the age and gender the range selection filters on;
        // that filtering belongs to ResultLimitService, so what matters here is
        // that this patient is the one handed to it.
        patient = mock(Patient.class);

        testSection = mock(TestSection.class);
        lenient().when(testSection.getId()).thenReturn("3");
        lenient().when(testSection.getLocalizedName()).thenReturn("Haematology");

        bloodPressure = mock(org.openelisglobal.test.valueholder.Test.class);
        lenient().when(bloodPressure.getId()).thenReturn(TEST_ID);
        lenient().when(bloodPressure.getLocalizedName()).thenReturn("Blood Pressure");
        lenient().when(bloodPressure.getTestSection()).thenReturn(testSection);

        lenient().when(patientService.get(PATIENT_ID)).thenReturn(patient);
        lenient().when(testService.getResultType(any())).thenReturn("N");
        lenient().when(testService.getPossibleTestResults(any())).thenReturn(new ArrayList<>());
        lenient().when(resultService.getSimpleResultValue(any(Result.class)))
                .thenAnswer(invocation -> ((Result) invocation.getArgument(0)).getValue());
    }

    @Test
    public void getResultTree_shouldKeepEachComponentOfATestAsItsOwnSeries() {
        TestResultComponent systolic = component("c-sys", "Systolic", true);
        TestResultComponent diastolic = component("c-dia", "Diastolic", false);
        when(testResultComponentService.getActiveComponentsByTestId(TEST_ID))
                .thenReturn(Arrays.asList(systolic, diastolic));

        TypeOfSample serum = sampleType("1", "Serum");
        Analysis analysis = analysis(bloodPressure, serum, Timestamp.valueOf("2026-08-01 09:00:00"));
        Result systolicResult = result(analysis, "120", systolic.getId());
        Result diastolicResult = result(analysis, "80", diastolic.getId());
        givenPatientResults(systolicResult, diastolicResult);

        when(resultLimitService.getResultLimitForResult(any(), any(), any(), eq("c-sys"))).thenReturn(limit(90d, 120d));
        when(resultLimitService.getResultLimitForResult(any(), any(), any(), eq("c-dia"))).thenReturn(limit(60d, 80d));
        when(resultLimitService.getDisplayReferenceRange(any(), anyString(), anyString())).thenReturn("range");

        List<TestDisplay> tests = onlyPanel(patientResultTreeService.getResultTree(PATIENT_ID)).getSubSets();

        assertEquals("each component is its own row", 2, tests.size());
        TestDisplay systolicDisplay = tests.get(0);
        TestDisplay diastolicDisplay = tests.get(1);

        assertEquals("Blood Pressure", systolicDisplay.getTestName());
        assertEquals("Systolic", systolicDisplay.getComponent());
        assertEquals("Serum", systolicDisplay.getSampleType());
        assertEquals("Blood Pressure — Systolic", systolicDisplay.getDisplay());
        assertEquals(1, systolicDisplay.getObs().size());
        assertEquals("120", systolicDisplay.getObs().get(0).getValue());
        assertEquals(Double.valueOf(120d), systolicDisplay.getHiNormal());

        assertEquals("Blood Pressure — Diastolic", diastolicDisplay.getDisplay());
        assertEquals(1, diastolicDisplay.getObs().size());
        assertEquals("80", diastolicDisplay.getObs().get(0).getValue());
        assertEquals(Double.valueOf(80d), diastolicDisplay.getHiNormal());
    }

    @Test
    public void getResultTree_shouldSplitOneTestAcrossItsSampleTypes() {
        when(testResultComponentService.getActiveComponentsByTestId(TEST_ID))
                .thenReturn(Collections.singletonList(component("c-primary", "Primary", true)));
        when(resultLimitService.getResultLimitForResult(any(), any(), any(), isNull())).thenReturn(limit(1d, 5d));
        when(resultLimitService.getDisplayReferenceRange(any(), anyString(), anyString())).thenReturn("1 - 5");

        TypeOfSample serum = sampleType("1", "Serum");
        TypeOfSample urine = sampleType("2", "Urine");
        givenPatientResults(
                result(analysis(bloodPressure, serum, Timestamp.valueOf("2026-08-01 09:00:00")), "3", null),
                result(analysis(bloodPressure, urine, Timestamp.valueOf("2026-08-02 09:00:00")), "4", null));

        List<ResultTree> trees = patientResultTreeService.getResultTree(PATIENT_ID);

        assertEquals(1, trees.size());
        List<PanelDisplay> panels = trees.get(0).getSubSets();
        assertEquals("one panel per sample type", 2, panels.size());
        assertEquals("Serum", panels.get(0).getDisplay());
        assertEquals("Serum", panels.get(0).getSubSets().get(0).getSampleType());
        assertEquals("Urine", panels.get(1).getDisplay());
        assertEquals("Urine", panels.get(1).getSubSets().get(0).getSampleType());
        assertNull("a single-component test is not qualified by a component",
                panels.get(0).getSubSets().get(0).getComponent());
        assertEquals("Blood Pressure", panels.get(0).getSubSets().get(0).getDisplay());
    }

    @Test
    public void getResultTree_shouldTakeTheRangeFromTheSharedPatientAwareSelection() {
        when(testResultComponentService.getActiveComponentsByTestId(TEST_ID)).thenReturn(new ArrayList<>());
        ResultLimit resultLimit = limit(4d, 9d);
        resultLimit.setLowCritical(2d);
        resultLimit.setHighCritical(12d);
        when(resultLimitService.getResultLimitForResult(any(), any(), any(), isNull())).thenReturn(resultLimit);
        when(resultLimitService.getDisplayReferenceRange(eq(resultLimit), anyString(), anyString()))
                .thenReturn("4 - 9");

        TypeOfSample serum = sampleType("1", "Serum");
        Analysis analysis = analysis(bloodPressure, serum, Timestamp.valueOf("2026-08-01 09:00:00"));
        Result recorded = result(analysis, "10", null);
        // The snapshot stored on the result row must not be what is displayed.
        recorded.setMinNormal(0d);
        recorded.setMaxNormal(100d);
        givenPatientResults(recorded);

        TestDisplay display = onlyPanel(patientResultTreeService.getResultTree(PATIENT_ID)).getSubSets().get(0);

        verify(resultLimitService).getResultLimitForResult(eq(analysis), eq(recorded), eq(patient), isNull());
        assertEquals(Double.valueOf(4d), display.getLowNormal());
        assertEquals(Double.valueOf(9d), display.getHiNormal());
        assertEquals(Double.valueOf(2d), display.getLowCritical());
        assertEquals(Double.valueOf(12d), display.getHiCritical());
        assertEquals("4 - 9", display.getRange());
    }

    @Test
    public void getResultTree_shouldLeaveUnconfiguredBoundsUnset() {
        when(testResultComponentService.getActiveComponentsByTestId(TEST_ID)).thenReturn(new ArrayList<>());
        ResultLimit resultLimit = limit(4d, 9d);
        when(resultLimitService.getResultLimitForResult(any(), any(), any(), isNull())).thenReturn(resultLimit);
        when(resultLimitService.getDisplayReferenceRange(any(), anyString(), anyString())).thenReturn("4 - 9");
        givenPatientResults(result(analysis(bloodPressure, sampleType("1", "Serum"),
                Timestamp.valueOf("2026-08-01 09:00:00")), "10", null));

        TestDisplay display = onlyPanel(patientResultTreeService.getResultTree(PATIENT_ID)).getSubSets().get(0);

        assertNull("an unset critical bound must not be folded onto the normal range", display.getLowCritical());
        assertNull(display.getHiCritical());
        assertNull(display.getLowAbsolute());
        assertNull(display.getHiAbsolute());
    }

    @Test
    public void getResultTree_shouldLineUpComponentsRecordedTogetherOnOneObservationTime() {
        TestResultComponent systolic = component("c-sys", "Systolic", true);
        TestResultComponent diastolic = component("c-dia", "Diastolic", false);
        when(testResultComponentService.getActiveComponentsByTestId(TEST_ID))
                .thenReturn(Arrays.asList(systolic, diastolic));
        when(resultLimitService.getResultLimitForResult(any(), any(), any(), anyString())).thenReturn(limit(1d, 2d));
        when(resultLimitService.getDisplayReferenceRange(any(), anyString(), anyString())).thenReturn("1 - 2");

        Analysis analysis = analysis(bloodPressure, sampleType("1", "Serum"), Timestamp.valueOf("2026-08-01 09:00:00"));
        Result systolicResult = result(analysis, "120", systolic.getId());
        systolicResult.setLastupdated(Timestamp.valueOf("2026-08-01 11:15:31"));
        Result diastolicResult = result(analysis, "80", diastolic.getId());
        diastolicResult.setLastupdated(Timestamp.valueOf("2026-08-01 11:15:47"));
        givenPatientResults(systolicResult, diastolicResult);

        List<TestDisplay> tests = onlyPanel(patientResultTreeService.getResultTree(PATIENT_ID)).getSubSets();

        assertEquals(tests.get(0).getObs().get(0).getObsDatetime(), tests.get(1).getObs().get(0).getObsDatetime());
    }

    @Test
    public void getResultTree_shouldSkipResultsReportedWithTheirParent() {
        when(testResultComponentService.getActiveComponentsByTestId(TEST_ID)).thenReturn(new ArrayList<>());
        when(resultLimitService.getResultLimitForResult(any(), any(), any(), isNull())).thenReturn(limit(1d, 5d));
        when(resultLimitService.getDisplayReferenceRange(any(), anyString(), anyString())).thenReturn("1 - 5");

        Analysis analysis = analysis(bloodPressure, sampleType("1", "Serum"),
                Timestamp.valueOf("2026-08-01 09:00:00"));
        Result parent = result(analysis, "3", null);
        Result quantification = result(analysis, "3.5", null);
        quantification.setParentResult(parent);
        givenPatientResults(parent, quantification);

        TestDisplay display = onlyPanel(patientResultTreeService.getResultTree(PATIENT_ID)).getSubSets().get(0);

        assertEquals(1, display.getObs().size());
        assertEquals("3", display.getObs().get(0).getValue());
    }

    @Test
    public void getTestResultTree_shouldNarrowToOneComponentWhenAsked() {
        TestResultComponent systolic = component("c-sys", "Systolic", true);
        TestResultComponent diastolic = component("c-dia", "Diastolic", false);
        when(testResultComponentService.getActiveComponentsByTestId(TEST_ID))
                .thenReturn(Arrays.asList(systolic, diastolic));
        when(testService.get(TEST_ID)).thenReturn(bloodPressure);
        when(resultLimitService.getResultLimitForResult(any(), any(), any(), anyString())).thenReturn(limit(1d, 2d));
        when(resultLimitService.getDisplayReferenceRange(any(), anyString(), anyString())).thenReturn("1 - 2");

        Analysis analysis = analysis(bloodPressure, sampleType("1", "Serum"), Timestamp.valueOf("2026-08-01 09:00:00"));
        givenPatientResults(result(analysis, "120", systolic.getId()), result(analysis, "80", diastolic.getId()));

        PanelDisplay panel = patientResultTreeService.getTestResultTree(PATIENT_ID, TEST_ID, "c-dia", null);

        assertNotNull(panel);
        assertEquals(1, panel.getSubSets().size());
        assertEquals("Diastolic", panel.getSubSets().get(0).getComponent());
        assertEquals("80", panel.getSubSets().get(0).getObs().get(0).getValue());
    }

    private PanelDisplay onlyPanel(List<ResultTree> trees) {
        assertEquals(1, trees.size());
        assertEquals(1, trees.get(0).getSubSets().size());
        return trees.get(0).getSubSets().get(0);
    }

    private void givenPatientResults(Result... results) {
        Sample sample = new Sample();
        sample.setId("100");
        when(sampleHumanService.getSamplesForPatient(PATIENT_ID)).thenReturn(Collections.singletonList(sample));
        when(resultService.getResultsForSample(sample)).thenReturn(Arrays.asList(results));
    }

    private TestResultComponent component(String id, String label, boolean primary) {
        TestResultComponent component = new TestResultComponent();
        component.setId(id);
        component.setTestId(TEST_ID);
        component.setLabel(label);
        component.setIsPrimary(primary);
        return component;
    }

    private TypeOfSample sampleType(String id, String name) {
        TypeOfSample sampleType = mock(TypeOfSample.class);
        lenient().when(sampleType.getId()).thenReturn(id);
        lenient().when(sampleType.getLocalizedName()).thenReturn(name);
        return sampleType;
    }

    private Analysis analysis(org.openelisglobal.test.valueholder.Test test, TypeOfSample sampleType,
            Timestamp completedDate) {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setTypeOfSample(sampleType);
        Analysis analysis = mock(Analysis.class);
        lenient().when(analysis.getTest()).thenReturn(test);
        lenient().when(analysis.getSampleItem()).thenReturn(sampleItem);
        lenient().when(analysis.getCompletedDate()).thenReturn(completedDate);
        return analysis;
    }

    private Result result(Analysis analysis, String value, String componentId) {
        Result result = new Result();
        result.setAnalysis(analysis);
        result.setValue(value);
        result.setResultType("N");
        result.setLastupdated(Timestamp.valueOf("2026-08-01 10:00:00"));
        TestResult testResult = new TestResult();
        testResult.setComponentId(componentId);
        result.setTestResult(testResult);
        return result;
    }

    private ResultLimit limit(Double lowNormal, Double highNormal) {
        ResultLimit resultLimit = new ResultLimit();
        resultLimit.setLowNormal(lowNormal);
        resultLimit.setHighNormal(highNormal);
        return resultLimit;
    }

    @Test
    public void getTestResultTree_shouldNarrowToOneSpecimen() {
        when(testResultComponentService.getActiveComponentsByTestId(TEST_ID)).thenReturn(new ArrayList<>());
        when(testService.get(TEST_ID)).thenReturn(bloodPressure);
        when(resultLimitService.getResultLimitForResult(any(), any(), any(), isNull())).thenReturn(limit(1d, 5d));
        when(resultLimitService.getDisplayReferenceRange(any(), anyString(), anyString())).thenReturn("1 - 5");

        givenPatientResults(
                result(analysis(bloodPressure, sampleType("1", "Serum"), Timestamp.valueOf("2026-08-01 09:00:00")), "3",
                        null),
                result(analysis(bloodPressure, sampleType("2", "Urine"), Timestamp.valueOf("2026-08-02 09:00:00")), "4",
                        null));

        PanelDisplay panel = patientResultTreeService.getTestResultTree(PATIENT_ID, TEST_ID, null, "2");

        assertEquals("a graph plots one specimen, not both", 1, panel.getSubSets().size());
        assertEquals("Urine", panel.getSubSets().get(0).getSampleType());
        assertEquals(1, panel.getSubSets().get(0).getObs().size());
        assertEquals("4", panel.getSubSets().get(0).getObs().get(0).getValue());
    }

    @Test
    public void getTestResultTree_shouldReturnObservationsNewestFirst() {
        when(testResultComponentService.getActiveComponentsByTestId(TEST_ID)).thenReturn(new ArrayList<>());
        when(testService.get(TEST_ID)).thenReturn(bloodPressure);
        when(resultLimitService.getResultLimitForResult(any(), any(), any(), isNull())).thenReturn(limit(1d, 5d));
        when(resultLimitService.getDisplayReferenceRange(any(), anyString(), anyString())).thenReturn("1 - 5");

        TypeOfSample serum = sampleType("1", "Serum");
        // Handed over oldest-first, as the result rows come back.
        givenPatientResults(
                result(analysis(bloodPressure, serum, Timestamp.valueOf("2026-08-01 09:00:00")), "3", null),
                result(analysis(bloodPressure, serum, Timestamp.valueOf("2026-08-03 09:00:00")), "5", null),
                result(analysis(bloodPressure, serum, Timestamp.valueOf("2026-08-02 09:00:00")), "4", null));

        List<ResultDisplay> obs = patientResultTreeService.getTestResultTree(PATIENT_ID, TEST_ID, null, null)
                .getSubSets().get(0).getObs();

        assertEquals(Arrays.asList("2026-08-03 09:00:00", "2026-08-02 09:00:00", "2026-08-01 09:00:00"),
                obs.stream().map(ResultDisplay::getObsDatetime).collect(Collectors.toList()));
    }
}
