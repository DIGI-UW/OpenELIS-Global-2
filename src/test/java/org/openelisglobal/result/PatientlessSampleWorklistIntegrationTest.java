package org.openelisglobal.result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A worklist that contains a sample with no patient must still load.
 *
 * <p>
 * A sample is joined to its patient through sample_human, and that join is
 * optional — {@code getPatientForSample} answers null where no row exists.
 * Every accessor on PatientService guards against a null patient except the
 * five that delegate to PersonService, which did not, even though PersonService
 * itself is null-safe throughout.
 *
 * <p>
 * So building a worklist row for such a sample threw
 * {@code NullPointerException: Cannot invoke "Patient.getPerson()" because
 * "patient" is null} out of the middle of the loop, and the whole section's
 * query went down with it — one patient-less sample made the worklist for its
 * lab unit return HTTP 500 while every other unit returned 200.
 */
public class PatientlessSampleWorklistIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultsLoadUtility resultsLoadUtility;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private PatientService patientService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/analysis.xml");
    }

    @Test
    public void aSampleWithNoPatientStillListsOnTheWorklist() {
        List<Analysis> analyses = analysisService.getAll();
        assertFalse("fixture must provide analyses", analyses.isEmpty());
        for (Analysis analysis : analyses) {
            assertNull("this fixture's samples are joined to no patient — the case under test",
                    sampleService.getPatient(analysis.getSampleItem().getSample()));
        }

        List<TestResultItem> items = resultsLoadUtility.getGroupedTestsForAnalysisList(analyses, true);

        assertNotNull("the worklist builds instead of throwing", items);
        assertFalse("and it lists the work rather than coming back empty", items.isEmpty());
    }

    /**
     * The name is simply absent, which is what the depersonalized branch of the
     * same loop has always produced for these samples.
     */
    @Test
    public void suchARowCarriesNoPatientNameRatherThanFailing() {
        List<TestResultItem> items = resultsLoadUtility.getGroupedTestsForAnalysisList(analysisService.getAll(), true);

        for (TestResultItem item : items) {
            assertEquals("no patient, so no name", "", item.getPatientName());
        }
    }

    @Test
    public void patientAccessorsAnswerForAnAbsentPatient() {
        assertEquals("", patientService.getLastFirstName(null));
        assertEquals("", patientService.getFirstName(null));
        assertEquals("", patientService.getLastName(null));
        assertEquals("", patientService.getPhone(null));
        assertNull(patientService.getPerson(null));
    }
}
