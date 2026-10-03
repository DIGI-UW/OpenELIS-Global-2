package org.openelisglobal.resultvalidation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1028 (Validation v4 slice V2) — the per-row review actions: Validate &
 * release one analysis (FR-D1) and Modify its result (FR-D4), each with the
 * dual-axis note (FR-F1). Fixture: {@code testdata/validation-review-panel.xml}
 * — accession VAL-RP-001 with analyses 100 and 102 awaiting validation.
 */
@Transactional
public class AccessionValidationReviewPanelTest extends BaseWebContextSensitiveTest {

    @PersistenceContext
    private EntityManager entityManager;

    private static final String ANALYSIS_ID = "100";
    private static final String SIBLING_ANALYSIS_ID = "102";

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    private MockHttpSession session;
    private String notesRequiredBefore;
    private String qcBlocksBefore;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/validation-review-panel.xml");
        authenticateAs("testUser");
        session = buildValidatorSession();
        notesRequiredBefore = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.notesRequiredForModifyResults);
        qcBlocksBefore = ConfigurationProperties.getInstance().getPropertyValue(Property.QC_FAIL_BLOCKS_VALIDATION);
    }

    @After
    public void restoreConfiguration() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.notesRequiredForModifyResults,
                notesRequiredBefore == null ? "false" : notesRequiredBefore);
        ConfigurationProperties.getInstance().setPropertyValue(Property.QC_FAIL_BLOCKS_VALIDATION,
                qcBlocksBefore == null ? "false" : qcBlocksBefore);
    }

    private MockHttpSession buildValidatorSession() {
        UserDetails userDetails = User.withUsername("testUser").password("N/A")
                .authorities("ROLE_ADMIN", "ROLE_VALIDATION").build();
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);
        return httpSession;
    }

    /** The row as the queue GET serves it, plus the panel's note and value. */
    private String rowBody(String value, String note, String visibility, String context) {
        return "{\"analysisId\":\"" + ANALYSIS_ID + "\",\"accessionNumber\":\"VAL-RP-001\",\"resultId\":\"100\","
                + "\"testId\":\"1\",\"resultType\":\"N\",\"result\":\"" + value + "\",\"note\":\"" + note + "\","
                + "\"noteVisibility\":\"" + visibility + "\",\"noteContext\":\"" + context + "\","
                + "\"isAccepted\":false,\"isRejected\":false}";
    }

    private List<Map<String, Object>> notesFor(String analysisId, String subject) {
        return jdbcTemplate.queryForList(
                "SELECT note_type, text FROM clinlims.note WHERE reference_id = ? AND subject = ?",
                Integer.valueOf(analysisId), subject);
    }

    @Test
    public void release_unknownAnalysis_returns404() throws Exception {
        mockMvc.perform(post("/rest/AccessionValidation/analysis/999999/release").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(rowBody("10.5", "", "", "VALIDATION")))
                .andExpect(status().isNotFound());
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    public void release_analysisNoLongerAwaitingValidation_returns409StalePage() throws Exception {
        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = ? WHERE id = 100",
                Integer.valueOf(statusService.getStatusID(AnalysisStatus.Finalized)));

        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/release").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(rowBody("10.5", "", "", "VALIDATION")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("notAwaitingValidation"));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    public void release_ofAResultAFailedControlHolds_isRefusedAndLeavesItAwaitingValidation() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.QC_FAIL_BLOCKS_VALIDATION, "true");
        QcHoldFixture.holdByAFailedControl(jdbcTemplate, ANALYSIS_ID);

        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/release").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(rowBody("10.5", "", "", "VALIDATION")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("qcHold"));
        entityManager.flush();
        entityManager.clear();

        Analysis held = analysisService.get(ANALYSIS_ID);
        assertEquals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance), held.getStatusId());
        assertNull(held.getReleasedDate());
    }

    @Test
    public void release_finalizesOnlyThatAnalysisAndStoresTheNoteWithTheChosenVisibility() throws Exception {
        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/release").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rowBody("10.5", "Reviewed against the previous run", "E", "VALIDATION")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("released"))
                .andExpect(jsonPath("$.analysisId").value("100"));
        entityManager.flush();
        entityManager.clear();

        Analysis released = analysisService.get(ANALYSIS_ID);
        assertEquals(statusService.getStatusID(AnalysisStatus.Finalized), released.getStatusId());
        assertNotNull("release must stamp released_date", released.getReleasedDate());

        Analysis sibling = analysisService.get(SIBLING_ANALYSIS_ID);
        assertEquals("the sibling row is untouched by a per-row release",
                statusService.getStatusID(AnalysisStatus.TechnicalAcceptance), sibling.getStatusId());
        assertNull(sibling.getReleasedDate());

        List<Map<String, Object>> notes = notesFor(ANALYSIS_ID, "Result Note (Validation)");
        assertEquals(1, notes.size());
        assertEquals("E", notes.get(0).get("note_type"));
        assertEquals("Reviewed against the previous run", notes.get(0).get("text"));
    }

    @Test
    public void release_withoutAChosenVisibility_keepsTheLegacyExternalDefaultForAcceptedRows() throws Exception {
        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/release").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(rowBody("10.5", "Legacy note", "", "")))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        List<Map<String, Object>> notes = notesFor(ANALYSIS_ID, "Result Note (Validation)");
        assertEquals(1, notes.size());
        assertEquals("E", notes.get(0).get("note_type"));
    }

    @Test
    public void modify_withoutAReasonWhenTheLabRequiresOne_returns400() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.notesRequiredForModifyResults, "true");

        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/modify").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(rowBody("12.25", "", "I", "MODIFICATION")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("modificationReasonRequired"));
        entityManager.flush();
        entityManager.clear();

        assertEquals("10.5",
                jdbcTemplate.queryForObject("SELECT value FROM clinlims.result WHERE id = 100", String.class));
    }

    @Test
    public void modify_withoutAValue_returns400() throws Exception {
        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/modify").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(rowBody("", "Cleared", "I", "MODIFICATION")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("resultRequired"));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    public void modify_updatesTheValueAdvancesTheRevisionAndKeepsTheRowAwaitingValidation() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.notesRequiredForModifyResults, "false");

        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/modify").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rowBody("12.25", "Transcription error", "I", "MODIFICATION"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("modified"));
        entityManager.flush();
        entityManager.clear();

        assertEquals("12.25",
                jdbcTemplate.queryForObject("SELECT value FROM clinlims.result WHERE id = 100", String.class));
        Analysis modified = analysisService.get(ANALYSIS_ID);
        assertEquals("a validator's modification must read as Modified in the queue", "2", modified.getRevision());
        assertEquals("the row stays awaiting validation", statusService.getStatusID(AnalysisStatus.TechnicalAcceptance),
                modified.getStatusId());
        assertNull(modified.getReleasedDate());

        List<Map<String, Object>> notes = notesFor(ANALYSIS_ID, "Result Note (Modification)");
        assertEquals(1, notes.size());
        assertEquals("I", notes.get(0).get("note_type"));
        assertEquals("Transcription error", notes.get(0).get("text"));
    }

    /**
     * OGC-1417 — a validator correcting a value into the critical range
     * acknowledges it before the correction is stored, as at Results Entry, and the
     * acknowledgement records the Validation page as where it was given.
     */
    @Test
    public void modify_toACriticalValue_isRefusedUntilAcknowledged_thenRecorded() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.notesRequiredForModifyResults, "false");
        jdbcTemplate.update("INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id, min_age, max_age,"
                + " low_normal, high_normal, low_valid, high_valid, low_reporting_range, high_reporting_range,"
                + " low_critical, high_critical, always_validate, lastupdated) VALUES (9501, 1, 4, 0, 'Infinity',"
                + " 5, 20, '-Infinity', 'Infinity', '-Infinity', 'Infinity', 2, 50, false, NOW())"
                + " ON CONFLICT (id) DO NOTHING");

        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/modify").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rowBody("75", "Transcription error", "I", "MODIFICATION")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.acknowledgementRequired[0].kind").value("CRITICAL"));
        assertEquals("10.5",
                jdbcTemplate.queryForObject("SELECT value FROM clinlims.result WHERE id = 100", String.class));

        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/modify").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rowBody("75", "Transcription error", "I", "MODIFICATION").replace("\"isAccepted\"",
                        "\"criticalAcknowledged\":true,\"isAccepted\"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("modified"));
        entityManager.flush();

        Map<String, Object> acknowledgement = jdbcTemplate
                .queryForMap("SELECT kind, source, result_id, result_value FROM clinlims.result_entry_acknowledgement"
                        + " WHERE analysis_id = 100");
        assertEquals("CRITICAL_ACKNOWLEDGED", acknowledgement.get("kind"));
        assertEquals("VALIDATION", acknowledgement.get("source"));
        assertEquals(100, ((Number) acknowledgement.get("result_id")).intValue());
        assertEquals("75", acknowledgement.get("result_value"));
    }

    /**
     * OGC-1417 — the batch validation save is a REST save too: a critical
     * correction posted through it is refused until it is acknowledged.
     */
    @Test
    public void batchSave_ofACriticalCorrection_isRefusedUntilAcknowledged() throws Exception {
        jdbcTemplate.update("INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id, min_age, max_age,"
                + " low_normal, high_normal, low_valid, high_valid, low_reporting_range, high_reporting_range,"
                + " low_critical, high_critical, always_validate, lastupdated) VALUES (9502, 1, 4, 0, 'Infinity',"
                + " 5, 20, '-Infinity', 'Infinity', '-Infinity', 'Infinity', 2, 50, false, NOW())"
                + " ON CONFLICT (id) DO NOTHING");
        mockMvc.perform(get("/rest/AccessionValidation").param("accessionNumber", "VAL-RP-001").session(session))
                .andExpect(status().isOk());
        // the batch save's stale-page check reads the result-entry page cache, which
        // a session that has opened Results Entry carries
        session.setAttribute(IActionConstants.RESULTS_SESSION_CACHE, new java.util.ArrayList<>(
                List.of(new java.util.ArrayList<>(List.of(new org.openelisglobal.result.valueholder.Result())))));

        String row = rowBody("75", "", "I", "VALIDATION").replace("\"isAccepted\":false", "\"isAccepted\":true");
        mockMvc.perform(post("/rest/AccessionValidation").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"resultList\":[" + row + "],\"paging\":{\"currentPage\":\"1\"}}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.acknowledgementRequired[0].kind").value("CRITICAL"));
        assertEquals("10.5",
                jdbcTemplate.queryForObject("SELECT value FROM clinlims.result WHERE id = 100", String.class));
    }
}
