package org.openelisglobal.resultrun.controller.rest;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import org.hibernate.StaleStateException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * OGC-1200: the run endpoints answer a user with Results rights in the run's
 * lab units, and hand back the accepted message as JSON, not as quoted text.
 */
public class ResultRunRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private MockHttpSession session;
    private long runId;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/result.xml");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO clinlims.analyzer (id, name, is_active, last_updated)"
                + " VALUES (9251, 'Run endpoint analyzer', true, NOW()) ON CONFLICT (id) DO NOTHING");
        jdbc.update("INSERT INTO clinlims.user_lab_unit_roles (system_user_id, last_updated) VALUES (1, NOW())"
                + " ON CONFLICT (system_user_id) DO NOTHING");
        jdbc.update("INSERT INTO clinlims.lab_unit_role_map (lab_unit_role_map_id, lab_unit) VALUES (-9351,"
                + " 'AllLabUnits') ON CONFLICT (lab_unit_role_map_id) DO NOTHING");
        jdbc.update("INSERT INTO clinlims.lab_unit_roles (system_user_id, lab_unit_role_map_id) VALUES (1, -9351)"
                + " ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO clinlims.lab_roles (lab_unit_role_map_id, role) VALUES (-9351, '5')"
                + " ON CONFLICT DO NOTHING");
        jdbc.update("DELETE FROM clinlims.analyzer_delivery_receipt WHERE id = 'receipt-run-endpoint'");
        jdbc.update("INSERT INTO clinlims.analyzer_delivery_receipt (id, connection_id, message_id, analyzer_id,"
                + " profile_id, profile_revision, results_staged, results_held, controls_processed, accepted_by,"
                + " accepted_at, bundle_json) VALUES ('receipt-run-endpoint', 'conn-run-endpoint', 'msg-run-endpoint',"
                + " 9251, 'profile', 1, 1, 0, 0, '1', NOW(), '{\"resourceType\":\"Bundle\",\"type\":\"message\"}')");
        runId = jdbc.queryForObject("SELECT nextval('clinlims.result_run_seq')", Long.class);
        jdbc.update("INSERT INTO clinlims.result_run (id, source, state, analyzer_id, delivery_receipt_id,"
                + " source_connection_id, source_message_id, row_count, opened_at, last_activity_at)"
                + " VALUES (?, 'ANALYZER', 'OPEN', 9251, 'receipt-run-endpoint', 'conn-run-endpoint',"
                + " 'msg-run-endpoint', 1, NOW(), NOW())", runId);
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE id = 'rrc-run-part'");
        jdbc.update("INSERT INTO clinlims.test_result_component (id, test_id, code, label, display_order, is_active,"
                + " lastupdated, is_primary) VALUES ('rrc-run-part', 1, 'LOG', 'Log viral load', 1, 'Y', NOW(), false)");
        jdbc.update("INSERT INTO clinlims.analyzer_results (id, analyzer_id, accession_number, test_name, result,"
                + " iscontrol, test_id, component_id, run_id, raw_sub_identity, last_updated)"
                + " VALUES (nextval('analyzer_results_seq'), 9251, '12345', 'Endpoint value', '91', false, 1,"
                + " 'rrc-run-part', ?, '', NOW())", runId);
        session = authenticatedSession();
    }

    @After
    public void removeRunData() {
        jdbc.update("DELETE FROM clinlims.analyzer_results WHERE run_id = ?", runId);
        jdbc.update("DELETE FROM clinlims.result_run WHERE id = ?", runId);
        jdbc.update("DELETE FROM clinlims.analyzer_delivery_receipt WHERE id = 'receipt-run-endpoint'");
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE id = 'rrc-run-part'");
        jdbc.update("DELETE FROM clinlims.analyzer WHERE id = 9251");
    }

    private static MockHttpSession authenticatedSession() {
        UserDetails userDetails = User.withUsername("admin").password("N/A").authorities("ROLE_ADMIN", "ROLE_RESULTS")
                .build();
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);
        return httpSession;
    }

    @Test
    public void theRawMessageIsReturnedAsJsonNotAsQuotedText() throws Exception {
        mockMvc.perform(get("/rest/result-runs/" + runId + "/raw").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Bundle")).andExpect(jsonPath("$.type").value("message"));
    }

    @Test
    public void theRunIsListedHeadedAndPagedForAUserWithResultsRights() throws Exception {
        mockMvc.perform(get("/rest/result-runs").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.runs[*].label").value(hasItem("RUN-" + runId)));
        mockMvc.perform(get("/rest/result-runs/" + runId).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("RUN-" + runId)).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rawAvailable").value(true));
        mockMvc.perform(get("/rest/result-runs/" + runId + "/rows?page=1&pageSize=25").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.rows[0].value").value("91"))
                .andExpect(jsonPath("$.rows[0].part").value("Log viral load"));
    }

    /**
     * OGC-1200 FR-B3, FR-C4: a control recorded on a row answers with the row's
     * hold after it, so the worklist holds the row on a failing control and
     * releases it once the run no longer holds its test, without a reload.
     */
    @Test
    public void aRowControlAnswersWithTheRowsHoldAfterIt() throws Exception {
        String failing = pendingAnalysis();
        String passing = pendingAnalysis();
        try {
            mockMvc.perform(post("/rest/result-runs/of-analysis/" + failing + "/controls").session(session)
                    .contentType(MediaType.APPLICATION_JSON).content(rowControl("INVALID")))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.runId").isNumber()).andExpect(jsonPath("$.waitingRun.held").value(true))
                    .andExpect(jsonPath("$.waitingRun.heldCause").value("QC_FAILED"));
            mockMvc.perform(post("/rest/result-runs/of-analysis/" + passing + "/controls").session(session)
                    .contentType(MediaType.APPLICATION_JSON).content(rowControl("VALID")))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.runId").isNumber())
                    .andExpect(jsonPath("$.waitingRun").doesNotExist());
        } finally {
            for (String analysisId : java.util.List.of(failing, passing)) {
                jdbc.update("DELETE FROM clinlims.result_run_event WHERE run_id IN (SELECT id FROM clinlims.result_run"
                        + " WHERE analysis_id = CAST(? AS numeric))", analysisId);
                jdbc.update("DELETE FROM clinlims.qc_result WHERE run_id IN (SELECT id FROM clinlims.result_run"
                        + " WHERE analysis_id = CAST(? AS numeric))", analysisId);
                jdbc.update("DELETE FROM clinlims.result_run WHERE analysis_id = CAST(? AS numeric)", analysisId);
                jdbc.update("DELETE FROM clinlims.analysis WHERE id = CAST(? AS numeric)", analysisId);
            }
        }
    }

    private String pendingAnalysis() {
        return jdbc.queryForObject("INSERT INTO clinlims.analysis (id, sampitem_id, test_id, test_sect_id, revision,"
                + " status_id, analysis_type, lastupdated) VALUES (nextval('clinlims.analysis_seq'), 601, 1, 1, 1, 1,"
                + " 'MANUAL', NOW()) RETURNING CAST(id AS VARCHAR)", String.class);
    }

    private static String rowControl(String outcome) {
        return "{\"source\": \"RDT\", \"testId\": \"1\", \"testSectionId\": \"1\", \"controlLabel\": \"Kit 1\","
                + " \"qualitativeOutcome\": \"" + outcome + "\"}";
    }

    /**
     * OGC-1200 slice 5b: confirming a retest link answers 400 with the reason when
     * the run retests nothing of the named run, and 404 for a run that does not
     * exist.
     */
    @Test
    public void aRetestLinkWithNothingToLinkIsRefusedWithTheReason() throws Exception {
        mockMvc.perform(post("/rest/result-runs/" + runId + "/tests/1/retest-of").session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"runId\": 1}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.CoreMatchers.containsString("does not retest")));
        mockMvc.perform(post("/rest/result-runs/999999999/tests/1/retest-of").session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"runId\": 1}")).andExpect(status().isNotFound());
    }

    /**
     * OGC-1200 slice 7 (FR-E4): an exception decision needs a reason (400), and one
     * taken on a value no longer under the record the page showed is refused as
     * stale (409), so the page reloads instead of offering it again.
     */
    @Test
    public void anExceptionDecisionNeedsAReasonAndIsRefusedAsStaleOnAChangedRecord() throws Exception {
        String rowId = jdbc.queryForObject("SELECT CAST(id AS VARCHAR) FROM clinlims.analyzer_results WHERE run_id = ?",
                String.class, runId);
        String body = "{\"rowIds\": [\"" + rowId + "\"], \"key\": \"UNMATCHED|NOT-THIS-ONE\", \"reason\": \"%s\"}";

        mockMvc.perform(post("/rest/result-runs/" + runId + "/exceptions/ignore").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(String.format(body, " ")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("A reason is required"));
        mockMvc.perform(post("/rest/result-runs/" + runId + "/exceptions/ignore").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(String.format(body, "Not ours")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.stale").value(true));
        mockMvc.perform(get("/rest/result-runs/" + runId + "/exceptions/orders?q=1234&testId=1").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].accessionNumber").value(hasItem("12345")));
        mockMvc.perform(post("/rest/result-runs/999999999/exceptions/hold").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(String.format(body, "r")))
                .andExpect(status().isNotFound());
    }

    /**
     * Two users resolving the same record at once: the second write is refused by
     * the row's optimistic lock, raised as Spring's, JPA's or Hibernate's exception
     * depending on where the flush happens, and is answered as stale (409) so the
     * page reloads, not as a server error.
     */
    @Test
    public void aWriteAnotherUsersWriteBeatIsAnsweredAsStale() {
        assertTrue(ResultRunRestController
                .isConcurrentChange(new ObjectOptimisticLockingFailureException(AnalyzerResults.class, "1")));
        assertTrue(ResultRunRestController.isConcurrentChange(
                new JpaSystemException(new RuntimeException(new StaleStateException("unexpected row count")))));
        assertTrue(ResultRunRestController
                .isConcurrentChange(new RollbackException(new OptimisticLockException("row changed"))));
        assertFalse(ResultRunRestController.isConcurrentChange(new RuntimeException("database unreachable")));
    }

    @Test
    public void aRunThatDoesNotExistIsNotFound() throws Exception {
        mockMvc.perform(get("/rest/result-runs/999999999").session(session)).andExpect(status().isNotFound());
        mockMvc.perform(get("/rest/result-runs/999999999/raw").session(session)).andExpect(status().isNotFound());
    }
}
