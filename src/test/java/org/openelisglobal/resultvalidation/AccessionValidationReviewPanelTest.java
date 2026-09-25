package org.openelisglobal.resultvalidation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/**
 * OGC-1028 (Validation v4 slice V2) — the per-row review actions: Validate &
 * release one analysis (FR-D1) and Modify its result (FR-D4), each with the
 * dual-axis note (FR-F1). Fixture: {@code testdata/validation-review-panel.xml}
 * — accession VAL-RP-001 with analyses 100 and 102 awaiting validation.
 */
public class AccessionValidationReviewPanelTest extends BaseWebContextSensitiveTest {

    private static final String ANALYSIS_ID = "100";
    private static final String SIBLING_ANALYSIS_ID = "102";

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    private MockHttpSession session;
    private String notesRequiredBefore;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/validation-review-panel.xml");
        authenticateAs("testUser");
        statusService.refreshCache();
        session = buildValidatorSession();
        notesRequiredBefore = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.notesRequiredForModifyResults);
    }

    @After
    public void restoreConfiguration() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.notesRequiredForModifyResults,
                notesRequiredBefore == null ? "false" : notesRequiredBefore);
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
    }

    @Test
    public void release_analysisNoLongerAwaitingValidation_returns409StalePage() throws Exception {
        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = ? WHERE id = 100",
                Integer.valueOf(statusService.getStatusID(AnalysisStatus.Finalized)));

        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/release").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(rowBody("10.5", "", "", "VALIDATION")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("notAwaitingValidation"));
    }

    @Test
    public void release_finalizesOnlyThatAnalysisAndStoresTheNoteWithTheChosenVisibility() throws Exception {
        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/release").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rowBody("10.5", "Reviewed against the previous run", "E", "VALIDATION")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("released"))
                .andExpect(jsonPath("$.analysisId").value("100"));

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

        assertEquals("10.5",
                jdbcTemplate.queryForObject("SELECT value FROM clinlims.result WHERE id = 100", String.class));
    }

    @Test
    public void modify_withoutAValue_returns400() throws Exception {
        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/modify").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(rowBody("", "Cleared", "I", "MODIFICATION")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("resultRequired"));
    }

    @Test
    public void modify_updatesTheValueAdvancesTheRevisionAndKeepsTheRowAwaitingValidation() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.notesRequiredForModifyResults, "false");

        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/modify").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rowBody("12.25", "Transcription error", "I", "MODIFICATION"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("modified"));

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
}
