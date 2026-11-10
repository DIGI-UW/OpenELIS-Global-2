package org.openelisglobal.result.service;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.reports.action.implementation.PatientCILNSPClinical_vreduit;
import org.openelisglobal.reports.action.implementation.PatientReport;
import org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class CodedResultNormalOptionIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String LIMIT_NORMAL = "9801";
    private static final String OPTION_NORMAL = "9802";
    private static final String NEITHER = "9803";
    private static final String NEITHER_BOUND_TO_NORMAL_OPTION = "9804";

    @Autowired
    private ResultService resultService;

    @Autowired
    private AnalysisService analysisService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/coded-result-normal-option.xml");
    }

    @Test
    public void onlyTheAnswerNeitherNamedByTheRangeNorMarkedNormalIsAbnormal() {
        Map<String, Boolean> abnormal = Map.of(LIMIT_NORMAL, isAbnormal(LIMIT_NORMAL), OPTION_NORMAL,
                isAbnormal(OPTION_NORMAL), NEITHER, isAbnormal(NEITHER), NEITHER_BOUND_TO_NORMAL_OPTION,
                isAbnormal(NEITHER_BOUND_TO_NORMAL_OPTION));

        assertEquals(
                Map.of(LIMIT_NORMAL, false, OPTION_NORMAL, false, NEITHER, true, NEITHER_BOUND_TO_NORMAL_OPTION, true),
                abnormal);
    }

    @Test
    public void patientReportFlagsOnlyTheAnswerNeitherNamedByTheRangeNorMarkedNormal() {
        PatientReport report = new PatientCILNSPClinical_vreduit();
        ReflectionTestUtils.setField(report, "currentAnalysis", analysisService.get("9800"));

        Map<String, Boolean> flagged = Map.of(LIMIT_NORMAL, reportFlag(report, LIMIT_NORMAL), OPTION_NORMAL,
                reportFlag(report, OPTION_NORMAL), NEITHER, reportFlag(report, NEITHER));

        assertEquals(Map.of(LIMIT_NORMAL, false, OPTION_NORMAL, false, NEITHER, true), flagged);
    }

    private boolean isAbnormal(String resultId) {
        return resultService.isAbnormalDictionaryResult(resultService.get(resultId));
    }

    private Boolean reportFlag(PatientReport report, String resultId) {
        ClinicalPatientData row = new ClinicalPatientData();
        ReflectionTestUtils.invokeMethod(report, "setAppropriateResults",
                new ArrayList<>(List.of(resultService.get(resultId))), row);
        return row.getAbnormalResult();
    }
}
