package org.openelisglobal.resultvalidation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.IdValuePair;
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
 * OGC-1030 (Validation v4 slice V4) — send for retest (FR-D3), reject via
 * non-conformity (FR-D2), the stale-page guard with its audit row (FR-J1) and
 * the read-only auto-validated list (FR-A4). Fixture:
 * {@code testdata/validation-review-panel.xml} — accession VAL-RP-001, analyses
 * 100 and 102 awaiting validation.
 */
public class AccessionValidationReviewActionsTest extends BaseWebContextSensitiveTest {

    private static final String ACCESSION = "VAL-RP-001";
    private static final String ANALYSIS_ID = "100";
    private static final String OTHER_ID = "102";

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    private MockHttpSession session;
    private String retestFlagBefore;
    private String rejectFlagBefore;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/validation-review-panel.xml");
        authenticateAs("testUser");
        statusService.refreshCache();
        jdbcTemplate.update("INSERT INTO clinlims.user_lab_unit_roles (system_user_id, last_updated) VALUES (1, NOW())"
                + " ON CONFLICT (system_user_id) DO NOTHING");
        jdbcTemplate.update("INSERT INTO clinlims.lab_unit_role_map (lab_unit_role_map_id, lab_unit) VALUES (9402,"
                + " 'AllLabUnits') ON CONFLICT (lab_unit_role_map_id) DO NOTHING");
        jdbcTemplate
                .update("INSERT INTO clinlims.lab_unit_roles (system_user_id, lab_unit_role_map_id) VALUES (1, 9402)"
                        + " ON CONFLICT DO NOTHING");
        jdbcTemplate.update("INSERT INTO clinlims.lab_roles (lab_unit_role_map_id, role)"
                + " SELECT 9402, CAST(id AS VARCHAR) FROM clinlims.system_role WHERE name = 'Validation'"
                + " ON CONFLICT DO NOTHING");
        DisplayListService displayList = webApplicationContext.getBean(DisplayListService.class);
        when(displayList.getList(DisplayListService.ListType.TEST_SECTION_ACTIVE))
                .thenReturn(List.of(new IdValuePair("1", "Environmental")));
        session = buildValidatorSession();
        retestFlagBefore = ConfigurationProperties.getInstance().getPropertyValue(Property.RETEST_NOTE_REQUIRED);
        rejectFlagBefore = ConfigurationProperties.getInstance().getPropertyValue(Property.allowResultRejection);
    }

    @After
    public void restoreConfiguration() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.RETEST_NOTE_REQUIRED,
                retestFlagBefore == null ? "false" : retestFlagBefore);
        ConfigurationProperties.getInstance().setPropertyValue(Property.allowResultRejection,
                rejectFlagBefore == null ? "false" : rejectFlagBefore);
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

    private String rowBody(String note, String visibility, String extraJson) {
        return "{\"analysisId\":\"" + ANALYSIS_ID + "\",\"accessionNumber\":\"" + ACCESSION + "\",\"resultId\":\"100\","
                + "\"testId\":\"1\",\"resultType\":\"N\",\"result\":\"10.5\",\"note\":\"" + note + "\","
                + "\"noteVisibility\":\"" + visibility + "\",\"noteContext\":\"VALIDATION\"" + extraJson + "}";
    }

    private List<Map<String, Object>> notesFor(String analysisId) {
        return jdbcTemplate.queryForList(
                "SELECT note_type, subject, text FROM clinlims.note WHERE reference_id = ? ORDER BY id",
                Integer.valueOf(analysisId));
    }

    private boolean hasNote(List<Map<String, Object>> notes, String text, String type) {
        return notes.stream().anyMatch(n -> text.equals(n.get("text")) && type.equals(n.get("note_type")));
    }

    // ---- retest (FR-D3) ------------------------------------------------------

    @Test
    public void retest_withoutANoteWhenTheLabRequiresOne_returns400() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.RETEST_NOTE_REQUIRED, "true");

        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/retest").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(rowBody("", "I", "")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("retestNoteRequired"));

        assertEquals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance),
                analysisService.get(ANALYSIS_ID).getStatusId());
    }

    @Test
    public void retest_sendsTheAnalysisBackToTheBenchWithTheLegacyAndValidatorNotes() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.RETEST_NOTE_REQUIRED, "false");

        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/retest").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(rowBody("Repeat with a fresh dilution", "I", "")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("retest"));

        Analysis analysis = analysisService.get(ANALYSIS_ID);
        assertEquals(statusService.getStatusID(AnalysisStatus.BiologistRejected), analysis.getStatusId());
        List<Map<String, Object>> notes = notesFor(ANALYSIS_ID);
        assertTrue("validator's note goes to the bench, internal", hasNote(notes, "Repeat with a fresh dilution", "I"));
        assertTrue("the legacy 'Redo test' note is still written",
                notes.stream().anyMatch(n -> "I".equals(n.get("note_type")) && "Result Note".equals(n.get("subject"))));
    }

    // ---- reject via NCE (FR-D2) ----------------------------------------------

    @Test
    public void reject_whenRejectionIsTurnedOff_returns403() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.allowResultRejection, "false");

        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/reject").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(rowBody("", "I", ",\"nceNumber\":\"NCE-1\"")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("rejectionDisabled"));
    }

    @Test
    public void reject_sendsTheAnalysisBackAndRecordsTheNonConformityNumber() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.allowResultRejection, "true");

        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/reject").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rowBody("Contaminated aliquot", "I", ",\"nceNumber\":\"NCE-2026-0007\"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("rejected"));

        Analysis analysis = analysisService.get(ANALYSIS_ID);
        assertEquals(statusService.getStatusID(AnalysisStatus.BiologistRejected), analysis.getStatusId());
        List<Map<String, Object>> notes = notesFor(ANALYSIS_ID);
        assertTrue("rejection note cites the NCE",
                notes.stream().anyMatch(n -> String.valueOf(n.get("text")).contains("NCE-2026-0007")
                        && "Result Note (Validation)".equals(n.get("subject"))));
        assertTrue(hasNote(notes, "Contaminated aliquot", "I"));
        assertEquals("the sibling row is untouched", statusService.getStatusID(AnalysisStatus.TechnicalAcceptance),
                analysisService.get(OTHER_ID).getStatusId());
    }

    // ---- stale-page guard (FR-J1) --------------------------------------------

    @Test
    public void release_withAStaleToken_returns409AndAuditsTheConflict() throws Exception {
        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/release").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(rowBody("", "E", ",\"analysisLastupdated\":\"1\"")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("stale"))
                .andExpect(jsonPath("$.analysisId").value(ANALYSIS_ID));

        assertEquals("a stale request must not release", statusService.getStatusID(AnalysisStatus.TechnicalAcceptance),
                analysisService.get(ANALYSIS_ID).getStatusId());
        Integer audits = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clinlims.history WHERE reference_id = ?"
                        + " AND convert_from(changes, 'UTF8') LIKE 'STALE_PAGE_CONFLICT_VALIDATION%'",
                Integer.class, Integer.valueOf(ANALYSIS_ID));
        assertEquals("the conflict is written to the audit trail", Integer.valueOf(1), audits);
    }

    @Test
    public void release_withTheCurrentToken_proceeds() throws Exception {
        long current = analysisService.get(ANALYSIS_ID).getLastupdated().getTime();

        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/release").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rowBody("", "E", ",\"analysisLastupdated\":\"" + current + "\""))).andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("released"));
    }

    // ---- auto-validated view (FR-A4) -----------------------------------------

    @Test
    public void autoValidated_listsFinalizedAnalysesWithoutAValidatorSignatureOnly() throws Exception {
        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = ?, released_date = NOW() WHERE id = 102",
                Integer.valueOf(statusService.getStatusID(AnalysisStatus.Finalized)));

        mockMvc.perform(
                get("/rest/AccessionValidation/auto-validated").param("accessionNumber", ACCESSION).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].analysisId").value(OTHER_ID))
                .andExpect(jsonPath("$[0].autoValidated").value(true)).andExpect(jsonPath("$[0].readOnly").value(true));
    }

    @Test
    public void autoValidated_unknownAccession_returns404() throws Exception {
        mockMvc.perform(
                get("/rest/AccessionValidation/auto-validated").param("accessionNumber", "NOPE").session(session))
                .andExpect(status().isNotFound());
    }
}
