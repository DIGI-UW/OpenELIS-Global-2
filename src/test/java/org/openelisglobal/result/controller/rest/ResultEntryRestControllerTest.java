package org.openelisglobal.result.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.test.service.TestSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * OGC-1020 (R1) — unified Results worklist REST surface: per-analysis save
 * scoping (FR-O1), optimistic stale-save rejection (FR-O2), session-bound
 * presence (FR-O3), and lab-unit domain (FR-M1).
 */
public class ResultEntryRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private MockHttpSession session;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/result.xml");
        jdbc = new JdbcTemplate(dataSource);
        seedAnalysisStatuses();
        jdbc.update("UPDATE clinlims.test_section SET domain = 'ENVIRONMENTAL' WHERE id = 2");
        // result.xml's panel rows omit lastupdated; a null @Version makes
        // Hibernate treat the referenced Panel as transient when the analysis
        // update cascades, failing the save.
        jdbc.update("UPDATE clinlims.panel SET lastupdated = NOW() WHERE lastupdated IS NULL");
        // R2 (OGC-1021): a real instrument, so analysis.analyzer_id's FK holds
        jdbc.update("INSERT INTO clinlims.analyzer (id, name, is_active, last_updated)"
                + " VALUES (9201, 'Leonardo', true, NOW()) ON CONFLICT (id) DO NOTHING");
        // R2 (OGC-1021): the worklist GET filters by lab-unit Results roles —
        // grant user 1 the Results role (id 5 in result.xml) on all lab units
        jdbc.update("INSERT INTO clinlims.user_lab_unit_roles (system_user_id, last_updated) VALUES (1, NOW())"
                + " ON CONFLICT (system_user_id) DO NOTHING");
        jdbc.update("INSERT INTO clinlims.lab_unit_role_map (lab_unit_role_map_id, lab_unit) VALUES (9301,"
                + " 'AllLabUnits') ON CONFLICT (lab_unit_role_map_id) DO NOTHING");
        jdbc.update("INSERT INTO clinlims.lab_unit_roles (system_user_id, lab_unit_role_map_id) VALUES (1, 9301)"
                + " ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO clinlims.lab_roles (lab_unit_role_map_id, role) VALUES (9301, '5')"
                + " ON CONFLICT DO NOTHING");
        statusService.refreshCache();
        session = buildAuthenticatedSession("admin");
    }

    /**
     * result.xml carries only the Finalized ANALYSIS status; the save path's status
     * transition needs the full named set (StatusService.addToAnalysisMap).
     */
    private void seedAnalysisStatuses() {
        String[][] statuses = { { "9101", "Not Tested", "4", "ANALYSIS" }, { "9102", "Test Canceled", "5", "ANALYSIS" },
                { "9103", "Technical Acceptance", "7", "ANALYSIS" }, { "9104", "Technical Rejected", "8", "ANALYSIS" },
                { "9105", "Biologist Rejection", "9", "ANALYSIS" }, { "9106", "Sample Rejected", "10", "ANALYSIS" },
                { "9107", "NonConforming", "11", "ANALYSIS" }, { "9108", "Test Entered", "12", "ORDER" },
                { "9109", "Testing Started", "13", "ORDER" }, { "9110", "Testing finished", "14", "ORDER" } };
        for (String[] status : statuses) {
            jdbc.update(
                    "INSERT INTO clinlims.status_of_sample (id, name, code, status_type, is_active, display_key,"
                            + " description, lastupdated) VALUES (?::numeric, ?, ?::numeric, ?, 'Y', ?, ?, NOW())",
                    status[0], status[1], status[2], status[3], "status." + status[0], status[1]);
        }
    }

    private MockHttpSession buildAuthenticatedSession(String loginName) {
        UserDetails userDetails = User.withUsername(loginName).password("N/A").authorities("ROLE_ADMIN", "ROLE_RESULTS")
                .build();
        SecurityContext sc = new SecurityContextImpl();
        sc.setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));

        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);

        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        return httpSession;
    }

    private String currentToken(String analysisId) {
        Timestamp lastupdated = analysisService.get(analysisId).getLastupdated();
        return String.valueOf(lastupdated.getTime());
    }

    private String saveBody(String analysisId, String resultId, String testId, String value, String token) {
        String accessionNumber = "1".equals(analysisId) ? "12345" : "13333";
        // combined "date time" — the format ResultsLoadUtility actually emits;
        // regression for the validateTestDate date-portion fix (OGC-1020)
        String testDate = org.openelisglobal.common.util.DateUtil.formatDateAsText(new java.util.Date()) + " 09:15";
        return "{\"testResult\":{" + "\"analysisId\":\"" + analysisId + "\"," + "\"accessionNumber\":\""
                + accessionNumber + "\"," + "\"resultId\":\"" + resultId + "\"," + "\"testId\":\"" + testId + "\","
                + "\"resultType\":\"N\"," + "\"resultValue\":\"" + value + "\"," + "\"testDate\":\"" + testDate + "\","
                + "\"isModified\":true," + "\"valid\":true," + "\"reportable\":true,"
                + (token == null ? "" : "\"analysisLastupdated\":\"" + token + "\",") + "\"note\":\"\"}}";
    }

    private String saveBodyWithExtras(String analysisId, String resultId, String testId, String value, String token,
            String extraJsonFields) {
        String base = saveBody(analysisId, resultId, testId, value, token);
        return base.replace("\"note\":\"\"}}", extraJsonFields + "}}");
    }

    @Test
    public void save_withCurrentToken_persistsValue_andNeverTouchesOtherAnalyses() throws Exception {
        String otherValueBefore = resultService.get("4").getValue();
        Timestamp otherLastupdatedBefore = analysisService.get("2").getLastupdated();

        mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                .content(saveBody("1", "3", "1", "90.0", currentToken("1"))).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.analysisLastupdated").exists());

        assertEquals("the edited result is persisted", "90.0", resultService.get("3").getValue());
        // FR-O1 regression (Lab Unit overwrite incident): the save wrote ONLY
        // the analysis it names — a colleague's row is untouched.
        assertEquals("another analysis' result is untouched", otherValueBefore, resultService.get("4").getValue());
        assertEquals("another analysis' version is untouched", otherLastupdatedBefore,
                analysisService.get("2").getLastupdated());
    }

    @Test
    public void save_withStaleToken_isRejected409_andWritesNothing() throws Exception {
        String staleToken = String.valueOf(analysisService.get("1").getLastupdated().getTime() - 60_000L);
        String valueBefore = resultService.get("3").getValue();

        mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                .content(saveBody("1", "3", "1", "90.0", staleToken)).session(session)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("error.results.staleSave"))
                .andExpect(jsonPath("$.modifiedBy").exists()).andExpect(jsonPath("$.analysisLastupdated").exists());

        // FR-O2: the stale editor loses — nothing was merged or overwritten.
        assertEquals(valueBefore, resultService.get("3").getValue());
    }

    @Test
    public void save_whosePayloadNamesAnotherAnalysis_isRejected400() throws Exception {
        mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                .content(saveBody("2", "4", "2", "15.0", null)).session(session)).andExpect(status().isBadRequest());
    }

    @Test
    public void presence_isVisibleToOtherSessions_andNeverToOwn() throws Exception {
        MockHttpSession sessionA = buildAuthenticatedSession("admin");
        MockHttpSession sessionB = buildAuthenticatedSession("jdoe");

        // A opens analysis 1 in Edit
        mockMvc.perform(post("/rest/results-entry/presence").contentType(MediaType.APPLICATION_JSON)
                .content("{\"analysisId\":\"1\",\"visibleAnalysisIds\":[\"1\",\"2\"]}").session(sessionA))
                .andExpect(status().isOk());

        // B sees A's claim on analysis 1 (FR-O3 advisory indicator)
        mockMvc.perform(post("/rest/results-entry/presence").contentType(MediaType.APPLICATION_JSON)
                .content("{\"analysisId\":null,\"visibleAnalysisIds\":[\"1\",\"2\"]}").session(sessionB))
                .andExpect(status().isOk()).andExpect(jsonPath("$.1").exists());

        // A never sees their own claim
        mockMvc.perform(post("/rest/results-entry/presence").contentType(MediaType.APPLICATION_JSON)
                .content("{\"analysisId\":\"1\",\"visibleAnalysisIds\":[\"1\",\"2\"]}").session(sessionA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.1").doesNotExist());
    }

    /**
     * OGC-1021 (R2, FR-B1/B2) — the instrument instance round-trips; a payload that
     * omits the field (legacy pages) never clears a stored analyzer; an explicit
     * blank clears it.
     */
    @Test
    public void save_persistsAnalyzer_absentFieldNeverClears_blankClears() throws Exception {
        mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON).content(
                saveBodyWithExtras("1", "3", "1", "90.0", currentToken("1"), "\"note\":\"\",\"analyzerId\":\"9201\""))
                .session(session)).andExpect(status().isOk());
        assertEquals("9201", analysisService.get("1").getAnalyzerId());

        // legacy save: no analyzerId field at all — must not clear
        mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                .content(saveBody("1", "3", "1", "91.0", currentToken("1"))).session(session))
                .andExpect(status().isOk());
        assertEquals("an absent analyzerId never clears the stored instrument", "9201",
                analysisService.get("1").getAnalyzerId());

        // explicit blank: the tech chose "no instrument"
        mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON).content(
                saveBodyWithExtras("1", "3", "1", "92.0", currentToken("1"), "\"note\":\"\",\"analyzerId\":\"\""))
                .session(session)).andExpect(status().isOk());
        assertEquals(null, analysisService.get("1").getAnalyzerId());
    }

    /**
     * OGC-1021 (R2, FR-J1/J2) — a send-with-result note persists as EXTERNAL with
     * the Modification context recorded in the subject.
     */
    @Test
    public void save_externalModificationNote_persistsBothAxes() throws Exception {
        mockMvc.perform(
                post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                        .content(saveBodyWithExtras("1", "3", "1", "90.0", currentToken("1"),
                                "\"note\":\"Corrected after re-run\",\"noteVisibility\":\"E\","
                                        + "\"noteContext\":\"MODIFICATION\""))
                        .session(session))
                .andExpect(status().isOk());

        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.note WHERE reference_id = 1 AND note_type = 'E'"
                        + " AND subject = 'Result Note (Modification)' AND text = 'Corrected after re-run'",
                Integer.class);
        assertEquals(Integer.valueOf(1), count);
    }

    /**
     * OGC-1021 (R2, FR-J1) — a note with no visibility/context sent (legacy pages)
     * keeps the legacy behavior byte-for-byte: INTERNAL, subject "Result Note".
     */
    @Test
    public void save_plainNote_keepsLegacyInternalDefault() throws Exception {
        mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                .content(saveBodyWithExtras("1", "3", "1", "90.0", currentToken("1"), "\"note\":\"routine comment\""))
                .session(session)).andExpect(status().isOk());

        Integer count = jdbc
                .queryForObject("SELECT count(*) FROM clinlims.note WHERE reference_id = 1 AND note_type = 'I'"
                        + " AND subject = 'Result Note' AND text = 'routine comment'", Integer.class);
        assertEquals(Integer.valueOf(1), count);
    }

    /**
     * OGC-1021 (R2, FR-D5) — a dilution save preserves the factor and measured
     * value as an internal provenance note alongside the reported value.
     */
    @Test
    public void save_withDilution_writesProvenanceNote() throws Exception {
        mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                .content(saveBodyWithExtras("1", "3", "1", "500", currentToken("1"),
                        "\"note\":\"\",\"dilutionFactor\":\"10\",\"measuredValue\":\"50\""))
                .session(session)).andExpect(status().isOk());

        assertEquals("500", resultService.get("3").getValue());
        Integer count = jdbc
                .queryForObject(
                        "SELECT count(*) FROM clinlims.note WHERE reference_id = 1 AND note_type = 'I'"
                                + " AND text LIKE 'Dilution factor 10%measured value 50%reported value 500%'",
                        Integer.class);
        assertEquals(Integer.valueOf(1), count);
    }

    /**
     * OGC-1021 (R2, FR-J1) — the worklist load surfaces saved notes as structured
     * items. Runs without a wrapping transaction (NOT_SUPPORTED), so the note's
     * lazy systemUser proxy is detached by load time — regression for the
     * LazyInitializationException that 500'd the whole worklist once any listed
     * analysis carried a note.
     */
    @Test
    public void worklistLoad_withSavedNote_returnsStructuredNotes_insteadOf500() throws Exception {
        mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                .content(saveBodyWithExtras("1", "3", "1", "90.0", currentToken("1"), "\"note\":\"panel note\""))
                .session(session)).andExpect(status().isOk());

        // put the order/analysis into statuses the unfinished-worklist query includes
        jdbc.update("UPDATE clinlims.sample SET status_id = 9109 WHERE id = 1");
        jdbc.update("UPDATE clinlims.analysis SET status_id = 9103 WHERE id = 1");

        // AppTestConfig's DisplayListService is a Mockito mock, and the lab-unit
        // role filter resolves the user's permitted sections against its
        // TEST_SECTION_ACTIVE list — stub it with the fixture's sections
        DisplayListService displayList = webApplicationContext.getBean(DisplayListService.class);
        when(displayList.getList(DisplayListService.ListType.TEST_SECTION_ACTIVE))
                .thenReturn(List.of(new IdValuePair("1", "TB"), new IdValuePair("2", "TestSection2")));
        try {
            mockMvc.perform(get("/rest/LogbookResults").param("labNumber", "12345").param("doRange", "false")
                    .param("finished", "false").session(session)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.testResult[0].analysisNotes[0].text").value("panel note"))
                    .andExpect(jsonPath("$.testResult[0].analysisNotes[0].noteType").value("I"))
                    .andExpect(jsonPath("$.testResult[0].analysisNotes[0].subject").value("Result Note"))
                    .andExpect(jsonPath("$.testResult[0].analysisNotes[0].author").value("Doe,John"));
        } finally {
            reset(displayList);
        }
    }

    /**
     * OGC-1022 (R3): numeric limits for test 1 — normal 70-100, critical <50 /
     * >400.
     */
    private void seedNumericResultLimit() {
        jdbc.update("INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id, min_age, max_age,"
                + " low_normal, high_normal, low_valid, high_valid, low_reporting_range, high_reporting_range,"
                + " low_critical, high_critical, always_validate, lastupdated) VALUES (9401, 1, 4, 0, 'Infinity',"
                + " 70, 100, '-Infinity', 'Infinity', '-Infinity', 'Infinity', 50, 400, false, NOW())"
                + " ON CONFLICT (id) DO NOTHING");
    }

    private org.springframework.test.web.servlet.ResultActions loadWorklistFor12345() throws Exception {
        jdbc.update("UPDATE clinlims.sample SET status_id = 9109 WHERE id = 1");
        jdbc.update("UPDATE clinlims.analysis SET status_id = 9103 WHERE id = 1");
        DisplayListService displayList = webApplicationContext.getBean(DisplayListService.class);
        when(displayList.getList(DisplayListService.ListType.TEST_SECTION_ACTIVE))
                .thenReturn(List.of(new IdValuePair("1", "TB"), new IdValuePair("2", "TestSection2")));
        return mockMvc.perform(get("/rest/LogbookResults").param("labNumber", "12345").param("doRange", "false")
                .param("finished", "false").session(session));
    }

    /**
     * OGC-1022 (R3, FR-L1 + FR-A4) — a save outside the authored critical bounds
     * flags the row CRITICAL on load and posts a CRITICAL_RESULT alert for the
     * analysis; a repeat critical save dedupes into the same alert.
     */
    @Test
    public void save_criticalValue_flagsRow_andPostsAlert() throws Exception {
        seedNumericResultLimit();
        mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                .content(saveBody("1", "3", "1", "500", currentToken("1"))).session(session))
                .andExpect(status().isOk());

        Integer alerts = jdbc.queryForObject("SELECT count(*) FROM clinlims.alert WHERE alert_type ="
                + " 'CRITICAL_RESULT' AND alert_entity_type = 'ANALYSIS' AND alert_entity_id = 1"
                + " AND status = 'OPEN'", Integer.class);
        assertEquals(Integer.valueOf(1), alerts);

        mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                .content(saveBody("1", "3", "1", "450", currentToken("1"))).session(session))
                .andExpect(status().isOk());
        Integer deduped = jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.alert WHERE alert_type ="
                        + " 'CRITICAL_RESULT' AND alert_entity_type = 'ANALYSIS' AND alert_entity_id = 1",
                Integer.class);
        assertEquals("a repeat critical save must not open a second alert", Integer.valueOf(1), deduped);

        try {
            loadWorklistFor12345().andExpect(status().isOk())
                    .andExpect(jsonPath("$.testResult[0].resultFlag").value("CRITICAL"))
                    .andExpect(jsonPath("$.testResult[0].criticalRange").value("< 50.00 or > 400.00"));
        } finally {
            reset(webApplicationContext.getBean(DisplayListService.class));
        }
    }

    @Test
    public void save_normalValue_flagsNormal_andPostsNoAlert() throws Exception {
        seedNumericResultLimit();
        mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                .content(saveBody("1", "3", "1", "90.0", currentToken("1"))).session(session))
                .andExpect(status().isOk());

        Integer alerts = jdbc.queryForObject("SELECT count(*) FROM clinlims.alert WHERE alert_type = 'CRITICAL_RESULT'",
                Integer.class);
        assertEquals(Integer.valueOf(0), alerts);

        try {
            loadWorklistFor12345().andExpect(status().isOk())
                    .andExpect(jsonPath("$.testResult[0].resultFlag").value("NORMAL"));
        } finally {
            reset(webApplicationContext.getBean(DisplayListService.class));
        }
    }

    /**
     * OGC-1022 (R3) — acknowledging a critical alert records the session user and
     * the resolution comment; the dashboard sends the comment under "notes".
     */
    @Test
    public void criticalAlert_acknowledge_recordsSessionUserAndComment() throws Exception {
        seedNumericResultLimit();
        mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                .content(saveBody("1", "3", "1", "500", currentToken("1"))).session(session))
                .andExpect(status().isOk());
        Long alertId = jdbc.queryForObject(
                "SELECT id FROM clinlims.alert WHERE alert_type = 'CRITICAL_RESULT' AND alert_entity_id = 1",
                Long.class);

        mockMvc.perform(put("/rest/alerts/dashboard/" + alertId + "/acknowledge")
                .contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"physician notified\"}").session(session))
                .andExpect(status().isOk());

        assertEquals("RESOLVED",
                jdbc.queryForObject("SELECT status FROM clinlims.alert WHERE id = " + alertId, String.class));
        assertEquals(Integer.valueOf(1),
                jdbc.queryForObject("SELECT acknowledged_by FROM clinlims.alert WHERE id = " + alertId, Integer.class));
        assertEquals("physician notified",
                jdbc.queryForObject("SELECT resolution_notes FROM clinlims.alert WHERE id = " + alertId, String.class));
    }

    /**
     * OGC-1022 (R3, FR-H1/H2) — the timeline endpoint serves this analysis's own
     * events (here: the note bound to it), normalizes off-menu page sizes to 25,
     * and 404s for an unknown analysis.
     */
    @Test
    public void analysisHistory_returnsOwnEvents_paginated() throws Exception {
        mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                .content(
                        saveBodyWithExtras("1", "3", "1", "90.0", currentToken("1"), "\"note\":\"history probe note\""))
                .session(session)).andExpect(status().isOk());

        mockMvc.perform(get("/rest/results-entry/analysis/1/history").param("pageSize", "7").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pageSize").value(25))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.events[?(@.type == 'NOTE')].detail", org.hamcrest.CoreMatchers
                        .hasItem(org.hamcrest.CoreMatchers.containsString("history probe note"))));

        mockMvc.perform(get("/rest/results-entry/analysis/99999/history").session(session))
                .andExpect(status().isNotFound());
    }

    /**
     * OGC-1023 (R4, FR-E3) — the reject disposition rides the legacy shadowRejected
     * mechanics: the value is cleared, the rejection reason is written as a note,
     * and the analysis moves to TechnicalRejected (VALIDATE_REJECTED_TESTS on) or
     * Canceled (off).
     */
    @Test
    public void save_withRejection_clearsValue_writesReasonNote_andCancels() throws Exception {
        DisplayListService displayList = webApplicationContext.getBean(DisplayListService.class);
        when(displayList.getList(DisplayListService.ListType.REJECTION_REASONS))
                .thenReturn(List.of(new IdValuePair("9501", "Specimen clotted")));
        try {
            mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                    .content(saveBodyWithExtras("1", "3", "1", "90.0", currentToken("1"),
                            "\"note\":\"\",\"rejected\":true,\"shadowRejected\":true,\"rejectReasonId\":\"9501\""))
                    .session(session)).andExpect(status().isOk());
        } finally {
            reset(displayList);
        }

        Integer reasonNotes = jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.note WHERE reference_id = 1" + " AND text = 'Specimen clotted'",
                Integer.class);
        assertEquals(Integer.valueOf(1), reasonNotes);
        assertEquals("9104",
                jdbc.queryForObject("SELECT status_id::text FROM clinlims.analysis WHERE id = 1", String.class));
    }

    /**
     * OGC-1023 (R4, FR-F2/F3) — a save carrying refer + referralItem creates the
     * Referral (with a non-null referral type — regression for the never-assigned
     * REFERRAL_CONFORMATION_ID that NULLed referral_type_id and 500'd the save) and
     * sets Analysis.referredOut. ResultsReferral is off in the default form field
     * set, so it is flipped for the duration of the test.
     */
    @Test
    public void save_withReferral_createsReferral_andSetsReferredOut() throws Exception {
        jdbc.update("INSERT INTO clinlims.referral_type (id, name, description, lastupdated) SELECT 9601,"
                + " 'Confirmation', 'Confirmation', NOW() WHERE NOT EXISTS"
                + " (SELECT 1 FROM clinlims.referral_type WHERE name = 'Confirmation')");
        jdbc.update("INSERT INTO clinlims.organization (id, name, is_active, lastupdated) VALUES (9801,"
                + " 'National Reference Laboratory', 'Y', NOW()) ON CONFLICT (id) DO NOTHING");

        @SuppressWarnings("unchecked")
        java.util.Map<org.openelisglobal.common.formfields.FormFields.Field, org.openelisglobal.common.formfields.FormField> formFields = (java.util.Map<org.openelisglobal.common.formfields.FormFields.Field, org.openelisglobal.common.formfields.FormField>) org.springframework.test.util.ReflectionTestUtils
                .getField(org.openelisglobal.common.formfields.FormFields.getInstance(), "fields");
        org.openelisglobal.common.formfields.FormField referralField = formFields
                .get(org.openelisglobal.common.formfields.FormFields.Field.ResultsReferral);
        Boolean before = referralField.getInUse();
        referralField.setInUse(true);
        try {
            mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                    .content(saveBodyWithExtras("1", "3", "1", "90.0", currentToken("1"),
                            "\"note\":\"\",\"refer\":true,\"referredOut\":true,\"referralItem\":"
                                    + "{\"referralReasonId\":\"1\",\"referredInstituteId\":\"9801\","
                                    + "\"referredSendDate\":\"07/08/2026\",\"referredTestId\":\"1\"}"))
                    .session(session)).andExpect(status().isOk());
        } finally {
            referralField.setInUse(before);
        }

        Integer referrals = jdbc.queryForObject("SELECT count(*) FROM clinlims.referral WHERE analysis_id = 1"
                + " AND referral_type_id IS NOT NULL AND organization_id = 9801", Integer.class);
        assertEquals(Integer.valueOf(1), referrals);
        assertEquals(Boolean.TRUE,
                jdbc.queryForObject("SELECT referred_out FROM clinlims.analysis WHERE id = 1", Boolean.class));
    }

    /**
     * OGC-1023 (R4) — the analysis timeline surfaces the referral (TEST_REFERRED)
     * and any non-conformity filed against this analysis (NCE_REPORTED).
     */
    @Test
    public void analysisHistory_includesReferralAndNceEvents() throws Exception {
        jdbc.update("INSERT INTO clinlims.referral_type (id, name, description, lastupdated) VALUES (9601,"
                + " 'Confirmation', 'Confirmation', NOW()) ON CONFLICT (id) DO NOTHING");
        jdbc.update("INSERT INTO clinlims.referral (id, analysis_id, organization_name, sent_date,"
                + " referral_type_id, canceled, lastupdated) VALUES (9602, 1, 'National Reference Lab', NOW(),"
                + " 9601, false, NOW())");
        jdbc.update("INSERT INTO clinlims.nc_event (id, nce_number, name, name_of_reporter, last_updated)"
                + " VALUES (9701, 'NCE-42', 'Broken tube', 'admin', NOW())");
        jdbc.update("INSERT INTO clinlims.nce_specimen (id, nce_id, sample_item_id, analysis_id, last_updated)"
                + " VALUES (9702, 9701, 601, 1, NOW())");

        mockMvc.perform(get("/rest/results-entry/analysis/1/history").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.events[?(@.type == 'REFERRAL')].detail",
                        org.hamcrest.CoreMatchers.hasItem("National Reference Lab")))
                .andExpect(jsonPath("$.events[?(@.type == 'NCE')].detail",
                        org.hamcrest.CoreMatchers.hasItem(org.hamcrest.CoreMatchers.containsString("NCE-42"))));
    }

    /**
     * OGC-1026 (R7, FR-G1) — an interpretation entered on the unified page rides
     * the save as an EXTERNAL note under the "Interpretation" subject, so it
     * reaches the patient report and the analysis timeline without new schema.
     */
    @Test
    public void save_withInterpretation_writesReportVisibleNote() throws Exception {
        mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                .content(saveBodyWithExtras("1", "3", "1", "90.0", currentToken("1"),
                        "\"note\":\"\",\"interpretation\":\"Fasting glucose within the reference range.\""))
                .session(session)).andExpect(status().isOk());

        Integer notes = jdbc.queryForObject("SELECT count(*) FROM clinlims.note WHERE reference_id = 1"
                + " AND note_type = 'E' AND subject = 'Interpretation'"
                + " AND text = 'Fasting glucose within the reference range.'", Integer.class);
        assertEquals(Integer.valueOf(1), notes);
    }

    @Test
    public void labUnitDomain_mapsFromTestSectionColumn() {
        // FR-M1 foundation: the OGC-1020 test_section.domain column round-trips
        // through the hbm mapping; unset rows default CLINICAL.
        assertEquals("ENVIRONMENTAL", testSectionService.get("2").getDomain());
        assertEquals("CLINICAL", testSectionService.get("1").getDomain());
    }

    /**
     * OGC-1021 (R2, FR-G) — interpretation rule buckets configured on the test's
     * components are readable through the RESULTS-role endpoint (the catalog
     * editor's own endpoint is ADMIN-only and would hide them from bench techs).
     */
    @Test
    public void testInterpretations_returnsActiveBucketsForResultsRole() throws Exception {
        jdbc.update("INSERT INTO clinlims.test_result_component (id, test_id, code, label, is_primary, is_active,"
                + " lastupdated) VALUES ('c-9401', 1, 'PRIMARY', 'Primary', true, 'Y', NOW())");
        jdbc.update("INSERT INTO clinlims.test_result_interpretation (id, component_id, value_match,"
                + " interpretation_text, severity, color, display_order, is_active, lastupdated)"
                + " VALUES ('i-9401', 'c-9401', '70-99', 'Normal fasting glucose.', 'NORMAL', 'green', 1, 'Y',"
                + " NOW())");
        jdbc.update("INSERT INTO clinlims.test_result_interpretation (id, component_id, value_match,"
                + " interpretation_text, severity, color, display_order, is_active, lastupdated)"
                + " VALUES ('i-9402', 'c-9401', '>=126', 'Diabetic range.', 'CRITICAL', 'red', 2, 'N', NOW())");

        mockMvc.perform(get("/rest/results-entry/test/1/interpretations").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].valueMatch").value("70-99"))
                .andExpect(jsonPath("$[0].text").value("Normal fasting glucose."))
                .andExpect(jsonPath("$[0].componentId").value("c-9401"));
    }

    /**
     * OGC-1024 (R5) — the test's reagent links (Test Catalog) are readable through
     * the RESULTS-role endpoint, enriched with the inventory item's name and units
     * so the bench section can render lots and record consumption.
     */
    @Test
    public void testReagentLinks_returnsCatalogLinksWithItemNames() throws Exception {
        jdbc.update("INSERT INTO clinlims.inventory_item (id, fhir_uuid, name, item_type, units, is_active,"
                + " last_updated) VALUES (9501, '11111111-1111-1111-1111-111111119501'::uuid, 'Glucose HK Gen.3',"
                + " 'REAGENT', 'mL', 'Y', NOW())");
        jdbc.update("INSERT INTO clinlims.test_reagent_link (id, test_id, reagent_id, usage_type, quantity_per_test,"
                + " quantity_unit, lastupdated) VALUES ('trl-9501', 1, 9501, 'PRIMARY', 1.5, 'mL', NOW())");

        mockMvc.perform(get("/rest/results-entry/test/1/reagents").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].reagentId").value(9501))
                .andExpect(jsonPath("$[0].name").value("Glucose HK Gen.3"))
                .andExpect(jsonPath("$[0].units").value("mL")).andExpect(jsonPath("$[0].usageType").value("PRIMARY"))
                .andExpect(jsonPath("$[0].quantityPerTest").value(1.5));

        mockMvc.perform(get("/rest/results-entry/test/2/reagents").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * Duplicate-component regression (unified page): a row saved from the blank
     * placeholder state posts resultId = "" — the legacy save service INSERTS on a
     * blank id, so a stale client duplicated the component's result on every save.
     * The endpoint now binds a blank-id item to the analysis's existing result for
     * that component (update, not insert), and returns the persisted resultId so
     * the client can adopt it.
     */
    @Test
    public void save_blankResultId_updatesExistingResultInsteadOfDuplicating() throws Exception {
        Integer before = jdbc.queryForObject("SELECT count(*) FROM clinlims.result WHERE analysis_id = 1",
                Integer.class);

        mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                .content(saveBody("1", "", "1", "91.0", currentToken("1"))).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultId").exists());

        Integer afterFirst = jdbc.queryForObject("SELECT count(*) FROM clinlims.result WHERE analysis_id = 1",
                Integer.class);
        assertEquals("blank-id save must UPDATE the existing result, not insert", before, afterFirst);

        mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                .content(saveBody("1", "", "1", "92.0", currentToken("1"))).session(session))
                .andExpect(status().isOk());
        Integer afterSecond = jdbc.queryForObject("SELECT count(*) FROM clinlims.result WHERE analysis_id = 1",
                Integer.class);
        assertEquals("repeat blank-id saves must stay idempotent", before, afterSecond);
        assertEquals("the latest value won", "92.0", resultService.get("3").getValue());
    }

    /**
     * The loader buckets legacy null-component results onto the PRIMARY component's
     * row — a blank-id save from that row must find them too.
     */
    @Test
    public void save_blankResultId_primaryComponentRow_matchesLegacyNullComponentResult() throws Exception {
        jdbc.update("INSERT INTO clinlims.test_result_component (id, test_id, code, label, is_primary, is_active,"
                + " lastupdated) VALUES ('c-dup-1', 1, 'PRIMARY', 'Primary', true, 'Y', NOW())");
        Integer before = jdbc.queryForObject("SELECT count(*) FROM clinlims.result WHERE analysis_id = 1",
                Integer.class);

        mockMvc.perform(post("/rest/results-entry/analysis/1/result").contentType(MediaType.APPLICATION_JSON)
                .content(saveBodyWithExtras("1", "", "1", "93.0", currentToken("1"),
                        "\"note\":\"\",\"testResultComponentId\":\"c-dup-1\""))
                .session(session)).andExpect(status().isOk());

        Integer after = jdbc.queryForObject("SELECT count(*) FROM clinlims.result WHERE analysis_id = 1",
                Integer.class);
        assertEquals("primary-row save must bind to the legacy null-component result", before, after);
        assertEquals("93.0", resultService.get("3").getValue());
    }
}
