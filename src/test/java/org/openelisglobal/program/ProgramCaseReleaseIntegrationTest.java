package org.openelisglobal.program;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.program.controller.cytology.CytologySampleForm;
import org.openelisglobal.program.controller.immunohistochemistry.ImmunohistochemistrySampleForm;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.service.ImmunohistochemistrySampleService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.cytology.CytologySampleService;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample.ImmunohistochemistryStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Releasing a program case finalizes the analyses of the case's own order.
 *
 * <p>
 * The fixture crosses the ids on purpose: sample 1 owns analysis 2 and sample 2
 * owns analysis 1, so a sample id used as an analysis id resolves to the other
 * order's analysis.
 */
public class ProgramCaseReleaseIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String CASE_SAMPLE_ID = "1";
    private static final String CASE_ANALYSIS_ID = "2";
    private static final String OTHER_ANALYSIS_ID = "1";

    @Autowired
    private PathologySampleService pathologySampleService;
    @Autowired
    private ImmunohistochemistrySampleService immunohistochemistrySampleService;
    @Autowired
    private CytologySampleService cytologySampleService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private IStatusService statusService;

    private String otherStatusBefore;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/program-case-release.xml");
        authenticateAs("technician1");
        Analysis caseAnalysis = analysisService.get(CASE_ANALYSIS_ID);
        Analysis otherAnalysis = analysisService.get(OTHER_ANALYSIS_ID);
        assertEquals("fixture: the case's analysis belongs to the case's sample", CASE_SAMPLE_ID,
                caseAnalysis.getSampleItem().getSample().getId());
        assertEquals("fixture: the other analysis belongs to the other order", "2",
                otherAnalysis.getSampleItem().getSample().getId());
        assertNull(caseAnalysis.getReleasedDate());
        assertNull(otherAnalysis.getReleasedDate());
        otherStatusBefore = otherAnalysis.getStatusId();
    }

    @Test
    public void pathologyRelease_finalizesOnlyTheCasesAnalysis() {
        PathologySampleForm form = new PathologySampleForm();
        form.setSystemUserId("1001");
        form.setStatus(PathologyStatus.COMPLETED);
        form.setRelease(true);
        form.setBlocks(new ArrayList<>());
        form.setSlides(new ArrayList<>());
        form.setReports(new ArrayList<>());

        pathologySampleService.updateWithFormValues(1, form);

        assertEquals(PathologyStatus.COMPLETED, pathologySampleService.get(1).getStatus());
        assertReleasedOnCaseAnalysisOnly();
    }

    @Test
    public void immunohistochemistryRelease_finalizesOnlyTheCasesAnalysis() {
        ImmunohistochemistrySampleForm form = new ImmunohistochemistrySampleForm();
        form.setSystemUserId("1001");
        form.setStatus(ImmunohistochemistryStatus.COMPLETED);
        form.setRelease(true);
        form.setReports(new ArrayList<>());

        immunohistochemistrySampleService.updateWithFormValues(2, form);

        assertEquals(ImmunohistochemistryStatus.COMPLETED, immunohistochemistrySampleService.get(2).getStatus());
        assertReleasedOnCaseAnalysisOnly();
    }

    @Test
    public void cytologyRelease_finalizesOnlyTheCasesAnalysis() {
        CytologySampleForm form = new CytologySampleForm();
        form.setSystemUserId("1001");
        form.setStatus(CytologyStatus.COMPLETED);
        form.setRelease(true);
        form.setSlides(new ArrayList<>());
        form.setReports(new ArrayList<>());

        cytologySampleService.updateWithFormValues(3, form);

        assertEquals(CytologyStatus.COMPLETED, cytologySampleService.get(3).getStatus());
        assertReleasedOnCaseAnalysisOnly();
    }

    private void assertReleasedOnCaseAnalysisOnly() {
        Analysis caseAnalysis = analysisService.get(CASE_ANALYSIS_ID);
        Analysis otherAnalysis = analysisService.get(OTHER_ANALYSIS_ID);

        assertNotNull("the case's own analysis is released", caseAnalysis.getReleasedDate());
        assertTrue("and finalized", statusService.matches(caseAnalysis.getStatusId(), AnalysisStatus.Finalized));
        assertEquals("a result is written on the case's analysis", Integer.valueOf(1), jdbcTemplate
                .queryForObject("SELECT COUNT(*) FROM clinlims.result WHERE analysis_id = 2", Integer.class));

        assertNull("the other order's analysis is not released", otherAnalysis.getReleasedDate());
        assertEquals("nor is its status changed", otherStatusBefore, otherAnalysis.getStatusId());
        assertEquals("nor does it gain a result", Integer.valueOf(0), jdbcTemplate
                .queryForObject("SELECT COUNT(*) FROM clinlims.result WHERE analysis_id = 1", Integer.class));
    }

}
