package org.openelisglobal.result.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    public void labUnitDomain_mapsFromTestSectionColumn() {
        // FR-M1 foundation: the OGC-1020 test_section.domain column round-trips
        // through the hbm mapping; unset rows default CLINICAL.
        assertEquals("ENVIRONMENTAL", testSectionService.get("2").getDomain());
        assertEquals("CLINICAL", testSectionService.get("1").getDomain());
    }
}
