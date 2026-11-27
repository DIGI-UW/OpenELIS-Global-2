package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.program.controller.pathology.PathologyController;
import org.openelisglobal.program.service.PathologyReportService;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.ReportView;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The final report a sign-out issues: the controller asks for it after the save
 * has committed, names it in the answer, and reports a failure there without
 * undoing the sign-out (design table S, D14). The template is the stub's.
 */
public class PathologySignOutReportTest extends PathologyControllerTestSupport {

    private static final String CASE_VIEW = "/rest/pathology/caseView/" + CASE_ID;

    /** technician1, whom the controller's requests are made as. */
    private static final String SESSION_USER_ID = "1001";

    private PathologyReportService pathologyReportService;
    private StubReportRenderer renderer;
    private StageSwitches switches;

    @Before
    public void stubTheTemplate() {
        switches = StageSwitches.allOn();
        pathologyReportService = webApplicationContext.getBean(PathologyReportService.class);
        clearReportRows();
        renderer = StubReportRenderer.installIn(pathologyReportService);
        jdbcTemplate.update("UPDATE clinlims.pathology_request SET status = 'COMPLETED' WHERE id = ?",
                FIXTURE_REQUEST_ID);
        storeAt(PathologyStatus.UNDER_REVIEW);
    }

    @After
    public void restoreTheTemplate() {
        renderer.restore();
        clearReportRows();
        switches.close();
    }

    // S1, S10
    @Test
    public void signingOut_issuesTheFinalReport_andNamesItInTheAnswer() throws Exception {
        MockHttpServletResponse response = signOut(",\"microscopyExam\":\"Chronic inflammation\"");

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertEquals("COMPLETED", storedStage());
        Map<String, Object> answer = bodyOf(response);
        assertEquals("the answer says the report step went through", Boolean.FALSE, answer.get("finalReportFailed"));
        @SuppressWarnings("unchecked")
        Map<String, Object> issued = (Map<String, Object>) answer.get("finalReport");
        assertEquals("FINAL", issued.get("type"));
        assertEquals("CURRENT", issued.get("state"));
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT report_type, generated_by, image FROM clinlims.pathology_report WHERE id = ?",
                ((Number) issued.get("id")).intValue());
        assertEquals("FINAL", row.get("report_type"));
        assertEquals("by the user who signed out", SESSION_USER_ID,
                String.valueOf(((Number) row.get("generated_by")).intValue()));
        assertTrue("stored with its file", row.get("image") != null);
        assertEquals("the reading the same save stored", "Chronic inflammation", jdbcTemplate.queryForObject(
                "SELECT microscopy_exam FROM clinlims.pathology_sample WHERE id = ?", String.class, CASE_ID));
    }

    // S12
    @Test
    public void aFailedReportStep_leavesTheCaseSignedOut_andSaysSo() throws Exception {
        renderer.fail = true;

        MockHttpServletResponse response = signOut("");

        assertEquals("the sign-out is still answered as done", HttpStatus.OK.value(), response.getStatus());
        Map<String, Object> answer = bodyOf(response);
        assertEquals("the answer says the report failed", Boolean.TRUE, answer.get("finalReportFailed"));
        assertNull("and names none", answer.get("finalReport"));
        assertEquals("the case is signed out", "COMPLETED", storedStage());
        assertTrue(pathologySampleService().isSignedOut(pathologySampleService().get(CASE_ID)));
        assertEquals("no report row is stored", 0, generatedRows().size());

        renderer.fail = false;
        ReportView later = pathologyReportService.generate(CASE_ID, SESSION_USER_ID, StubReportRenderer.PATHS);
        assertEquals("a later generate issues the final", "FINAL", later.type());
    }

    /**
     * A colleague who reopens the case between the sign-out's commit and its report
     * step leaves that step a draft: the answer says the report failed and names no
     * final.
     */
    @Test
    public void aReportStepThatIssuesNoFinal_isAFailedReportStep() throws Exception {
        // The controller is built in the security context over the parent's services.
        PathologyController controller = AopTestUtils
                .getUltimateTargetObject(securityContext.getBean(PathologyController.class));
        Object ownService = ReflectionTestUtils.getField(controller, "pathologyReportService");
        ReflectionTestUtils.setField(controller, "pathologyReportService",
                reopenedBeforeTheReportStep(pathologyReportService));
        MockHttpServletResponse response;
        try {
            response = signOut("");
        } finally {
            ReflectionTestUtils.setField(controller, "pathologyReportService", ownService);
        }

        Map<String, Object> answer = bodyOf(response);
        assertEquals("setup: the report step stored a draft", List.of("DRAFT"),
                jdbcTemplate.queryForList(
                        "SELECT report_type FROM clinlims.pathology_report WHERE pathology_sample_id = ? AND id <> 601",
                        String.class, CASE_ID));
        assertEquals("the answer says the report failed", Boolean.TRUE, answer.get("finalReportFailed"));
        assertNull("and names no final", answer.get("finalReport"));
    }

    // S13
    @Test
    public void theFinal_takesTheNextNumber_andTheDraftReadsSuperseded() throws Exception {
        ReportView draft = pathologyReportService.generate(CASE_ID, SESSION_USER_ID, StubReportRenderer.PATHS);
        assertEquals("setup: a draft before the sign-out", "DRAFT", draft.type());

        signOut("");

        List<ReportView> views = pathologyReportService.listFor(CASE_ID);
        ReportView fin = views.get(0);
        assertEquals("FINAL", fin.type());
        assertEquals("the final takes the next number", Integer.valueOf(draft.version() + 1), fin.version());
        assertEquals("the draft reads superseded", ReportView.SUPERSEDED,
                views.stream().filter(view -> view.id().equals(draft.id())).findFirst().orElseThrow().state());
    }

    // R18
    @Test
    public void aFinalIssuedAfterAReopen_isAskedForItsCorrectedLine() throws Exception {
        signOut("");
        assertEquals("setup: the case is reopened", HttpStatus.OK.value(),
                postAsTechnician(CASE_VIEW + "/reopen", "{\"from\":\"COMPLETED\",\"reason\":\"Amended diagnosis\"}")
                        .getStatus());

        signOut("");

        assertEquals("the first final is not amended, the one after the reopen is", List.of(false, true),
                renderer.amendedSeen);
    }

    // R18
    @Test
    public void aFirstFinal_andADraft_areNot() throws Exception {
        pathologyReportService.generate(CASE_ID, SESSION_USER_ID, StubReportRenderer.PATHS);
        signOut("");

        assertEquals("neither the draft nor the first final is amended", List.of(false, false), renderer.amendedSeen);
    }

    // helpers

    private MockHttpServletResponse signOut(String extraFields) throws Exception {
        MockHttpServletResponse response = postAsTechnician(CASE_VIEW,
                "{\"grossExam\":\"Normal tissues observed\",\"blocks\":[],\"reports\":[],\"slides\":[],"
                        + "\"release\":true" + extraFields + "}");
        assertEquals("setup: the sign-out is answered", HttpStatus.OK.value(), response.getStatus());
        return response;
    }

    /**
     * The report service as the controller sees it when another pathologist reopens
     * the case just before the report step runs.
     */
    private PathologyReportService reopenedBeforeTheReportStep(PathologyReportService reportService) {
        return (PathologyReportService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { PathologyReportService.class }, (proxy, method, args) -> {
                    if ("generate".equals(method.getName())) {
                        pathologySampleService().reopen(CASE_ID, PathologyStatus.COMPLETED, "Amended diagnosis",
                                SESSION_USER_ID);
                    }
                    try {
                        return method.invoke(reportService, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    private org.openelisglobal.program.service.PathologySampleService pathologySampleService() {
        return webApplicationContext.getBean(org.openelisglobal.program.service.PathologySampleService.class);
    }

    private List<Map<String, Object>> generatedRows() {
        return jdbcTemplate.queryForList("SELECT id FROM clinlims.pathology_report WHERE pathology_sample_id = ?"
                + " AND report_type IN ('DRAFT', 'FINAL')", CASE_ID);
    }

    /** The report rows and their audit; the fixture's legacy row 601 stays. */
    private void clearReportRows() {
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_table ="
                + " (SELECT id FROM clinlims.reference_tables WHERE name = 'PATHOLOGY_REPORT')");
        jdbcTemplate.update("DELETE FROM clinlims.pathology_report WHERE pathology_sample_id = ? AND id <> 601",
                CASE_ID);
    }
}
