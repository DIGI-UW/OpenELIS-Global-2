package org.openelisglobal.program;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.exception.LocalizedValidationException;
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
import org.openelisglobal.program.valueholder.pathology.PathologyStageEvent;
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
    private static final int PATHOLOGY_CASE_ID = 1;

    /** This class's own rows, outside the ids the fixture uses. */
    private static final int REFERRAL_SECTION_ID = 9501;
    private static final int REFERRAL_ANALYSIS_ID = 9501;

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
        removeOwnRows();
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
        assertEquals("release records the case's entry onto Completed, as any other move does",
                List.of(PathologyStatus.COMPLETED), pathologySampleService.getStageEvents(1).stream()
                        .map(PathologyStageEvent::getStage).collect(Collectors.toList()));
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

    @After
    public void cleanUp() {
        removeOwnRows();
    }

    /**
     * A case set to Completed without a release, its analyses still unreleased, is
     * released once. The release stamps the case's analysis, and the case is not
     * offered a second.
     */
    @Test
    public void pathologyCaseLeftAtCompletedWithUnreleasedAnalyses_isReleasedOnce() {
        leavePathologyCaseAtCompleted();
        assertTrue("a case never released reads as releasable", isPathologyCaseReleasable());

        pathologySampleService.updateWithFormValues(PATHOLOGY_CASE_ID, pathologyReleaseForm());

        assertNotNull("the release stamps the case's analysis",
                analysisService.get(CASE_ANALYSIS_ID).getReleasedDate());
        assertFalse("and the case no longer reads as releasable", isPathologyCaseReleasable());
        assertSecondPathologyReleaseRefused();
    }

    /**
     * A case released before releases were audited has no sign-out entry and no
     * entry onto Completed. The released date its release stamped on the case's
     * analysis is what records it.
     */
    @Test
    public void pathologyCaseReleasedBeforeReleasesWereAudited_isRefusedASecondRelease() {
        leavePathologyCaseAtCompleted();
        jdbcTemplate.update("UPDATE clinlims.analysis SET released_date = now() WHERE id = ?",
                Integer.parseInt(CASE_ANALYSIS_ID));

        assertFalse("the case reads as released", isPathologyCaseReleasable());
        assertSecondPathologyReleaseRefused();
        assertEquals("the refused release writes no result on the case's analysis", Integer.valueOf(0), jdbcTemplate
                .queryForObject("SELECT COUNT(*) FROM clinlims.result WHERE analysis_id = 2", Integer.class));
    }

    /**
     * An immunohistochemistry referral adds its own analysis to the case's order.
     * That analysis being released is the referral's release, not the case's.
     */
    @Test
    public void releasedImmunohistochemistryAnalysis_isNotThePathologyCasesRelease() {
        leavePathologyCaseAtCompleted();
        jdbcTemplate.update("INSERT INTO clinlims.test_section (id, name, description, org_id, is_external,"
                + " lastupdated, sort_order, name_localization_id, display_key, domain)"
                + " SELECT ?, 'Immunohistochemistry', 'Referral section', org_id, is_external, now(), sort_order,"
                + " name_localization_id, 'ReferralSectionKey', domain FROM clinlims.test_section WHERE id = 2",
                REFERRAL_SECTION_ID);
        jdbcTemplate.update(
                "INSERT INTO clinlims.analysis (id, sampitem_id, test_sect_id, test_id, revision,"
                        + " status_id, analysis_type, lastupdated, started_date, released_date, type_of_sample_name,"
                        + " fhir_uuid, parent_analysis_id, referred_out, corrected, result_calculated, reflex_trigger,"
                        + " method_id) SELECT ?, sampitem_id, ?, test_id, revision, status_id, analysis_type, now(),"
                        + " started_date, now(), type_of_sample_name, gen_random_uuid(), id, false, corrected,"
                        + " result_calculated, reflex_trigger, method_id FROM clinlims.analysis WHERE id = 2",
                REFERRAL_ANALYSIS_ID, REFERRAL_SECTION_ID);

        assertTrue("the case was never released, so it still reads as releasable", isPathologyCaseReleasable());
    }

    private void leavePathologyCaseAtCompleted() {
        jdbcTemplate.update("UPDATE clinlims.pathology_sample SET status = 'COMPLETED' WHERE id = ?",
                PATHOLOGY_CASE_ID);
    }

    private boolean isPathologyCaseReleasable() {
        return pathologySampleService.isReleasable(pathologySampleService.get(PATHOLOGY_CASE_ID));
    }

    private PathologySampleForm pathologyReleaseForm() {
        PathologySampleForm form = new PathologySampleForm();
        form.setSystemUserId("1001");
        form.setRelease(true);
        form.setBlocks(new ArrayList<>());
        form.setSlides(new ArrayList<>());
        form.setReports(new ArrayList<>());
        return form;
    }

    private void assertSecondPathologyReleaseRefused() {
        LocalizedValidationException refusal = assertThrows(LocalizedValidationException.class,
                () -> pathologySampleService.updateWithFormValues(PATHOLOGY_CASE_ID, pathologyReleaseForm()));
        assertEquals("refused as already signed out", "pathology.error.caseCompleted", refusal.getErrorCode());
    }

    /**
     * The referral rows, and the pathology case's stage entries and audit entries,
     * which outlive a fixture reload.
     */
    private void removeOwnRows() {
        jdbcTemplate.update("DELETE FROM clinlims.analysis WHERE id = ?", REFERRAL_ANALYSIS_ID);
        jdbcTemplate.update("DELETE FROM clinlims.test_section WHERE id = ?", REFERRAL_SECTION_ID);
        jdbcTemplate.update("DELETE FROM clinlims.pathology_stage_event WHERE pathology_sample_id = ?",
                PATHOLOGY_CASE_ID);
        jdbcTemplate.update(
                "DELETE FROM clinlims.history WHERE reference_id = ? AND reference_table ="
                        + " (SELECT id FROM clinlims.reference_tables WHERE name = 'PATHOLOGY_SAMPLE')",
                PATHOLOGY_CASE_ID);
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
