package org.openelisglobal.resultvalidation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.resultvalidation.util.ResultsValidationUtility;
import org.openelisglobal.sample.service.SampleService;
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
 * OGC-1029 (Validation v4 slice V3) — the guarded bulk release: the server
 * re-derives the Clear lane from its own load of the queue and releases only
 * the requested analyses that are clear, under the "allow bulk release of clear
 * results" flag. Fixture: {@code testdata/validation-bulk-release.xml} —
 * accession VAL-BR-001; analysis 100 is clear (10.5 in 5.0 - 20.0, QC passed),
 * analysis 102 is abnormal (25.0).
 */
public class AccessionValidationBulkReleaseTest extends BaseWebContextSensitiveTest {

    private static final String ACCESSION = "VAL-BR-001";
    private static final String CLEAR_ID = "100";
    private static final String ABNORMAL_ID = "102";

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private ResultsValidationUtility validationUtility;

    @Autowired
    private org.openelisglobal.typeoftestresult.service.TypeOfTestResultService typeOfTestResultService;

    @Autowired
    private org.openelisglobal.resultlimit.service.ResultLimitService resultLimitService;

    private MockHttpSession session;
    private String bulkFlagBefore;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/validation-bulk-release.xml");
        // "All ages" limits carry max_age = Infinity (ResultLimit.ageLimitsAreDefault);
        // DBUnit cannot write that literal, so the fixture's placeholder is replaced
        // here. The limit must also point at the Numeric result type: other fixtures
        // in the same JVM replace the type rows, and ResultLimitServiceImpl caches the
        // Numeric id at @PostConstruct — so resolve the current row and re-run that
        // initialiser to keep the two in step whatever ran before this class.
        org.openelisglobal.typeoftestresult.valueholder.TypeOfTestResult numeric = typeOfTestResultService
                .getTypeOfTestResultByType("N");
        if (numeric == null) {
            jdbcTemplate.update("INSERT INTO clinlims.type_of_test_result (id, description, test_result_type,"
                    + " hl7_value, lastupdated) VALUES ((SELECT COALESCE(MAX(id), 0) + 1 FROM"
                    + " clinlims.type_of_test_result), 'Numeric', 'N', 'NM', NOW())");
            numeric = typeOfTestResultService.getTypeOfTestResultByType("N");
        }
        jdbcTemplate.update(
                "UPDATE clinlims.result_limits SET max_age = 'Infinity', test_result_type_id = ? WHERE id = 1",
                Integer.valueOf(numeric.getId()));
        ((org.openelisglobal.resultlimit.service.ResultLimitServiceImpl) org.springframework.test.util.AopTestUtils
                .getTargetObject(resultLimitService)).initializeGlobalVariables();
        authenticateAs("testUser");
        statusService.refreshCache();
        // The queue is filtered by the validator's lab-unit roles: grant the
        // Validation role on every lab unit (as ResultEntryRestControllerTest does).
        jdbcTemplate.update("INSERT INTO clinlims.user_lab_unit_roles (system_user_id, last_updated) VALUES (1, NOW())"
                + " ON CONFLICT (system_user_id) DO NOTHING");
        jdbcTemplate.update("INSERT INTO clinlims.lab_unit_role_map (lab_unit_role_map_id, lab_unit) VALUES (9401,"
                + " 'AllLabUnits') ON CONFLICT (lab_unit_role_map_id) DO NOTHING");
        jdbcTemplate
                .update("INSERT INTO clinlims.lab_unit_roles (system_user_id, lab_unit_role_map_id) VALUES (1, 9401)"
                        + " ON CONFLICT DO NOTHING");
        jdbcTemplate.update("INSERT INTO clinlims.lab_roles (lab_unit_role_map_id, role)"
                + " SELECT 9401, CAST(id AS VARCHAR) FROM clinlims.system_role WHERE name = 'Validation'"
                + " ON CONFLICT DO NOTHING");
        // DisplayListService is a Mockito mock in the test profile (AppTestConfig), so
        // the active-section list the filter consults has to be stubbed explicitly.
        DisplayListService displayList = webApplicationContext.getBean(DisplayListService.class);
        when(displayList.getList(DisplayListService.ListType.TEST_SECTION_ACTIVE))
                .thenReturn(List.of(new IdValuePair("1", "Environmental")));
        session = buildValidatorSession();
        bulkFlagBefore = ConfigurationProperties.getInstance().getPropertyValue(Property.ALLOW_BULK_RELEASE_CLEAR);
        ConfigurationProperties.getInstance().setPropertyValue(Property.ALLOW_BULK_RELEASE_CLEAR, "true");
    }

    @After
    public void restoreConfiguration() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.ALLOW_BULK_RELEASE_CLEAR,
                bulkFlagBefore == null ? "true" : bulkFlagBefore);
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

    private String requestBody(String... rowsJson) {
        return "{\"accessionNumber\":\"" + ACCESSION + "\",\"doRange\":false,\"rows\":[" + String.join(",", rowsJson)
                + "]}";
    }

    private String rowJson(String analysisId, String note, String visibility) {
        return "{\"analysisId\":\"" + analysisId + "\",\"accessionNumber\":\"" + ACCESSION + "\",\"note\":\"" + note
                + "\",\"noteVisibility\":\"" + visibility + "\",\"noteContext\":\"VALIDATION\"}";
    }

    @Test
    public void theQueueGetServesBothRowsToThisValidator() throws Exception {
        mockMvc.perform(get("/rest/AccessionValidation").param("accessionNumber", ACCESSION).param("doRange", "false")
                .session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.resultList.length()").value(2));
    }

    @Test
    public void theQueueItselfPutsOnlyTheInRangeRowInTheClearLane() {
        List<AnalysisItem> rows = validationUtility
                .getValidationAnalysisBySample(sampleService.getSampleByAccessionNumber(ACCESSION));
        assertEquals(2, rows.size());
        for (AnalysisItem row : rows) {
            boolean clear = org.openelisglobal.resultvalidation.util.ValidationSignals.isClear(row);
            if (CLEAR_ID.equals(row.getAnalysisId())) {
                assertTrue("10.5 inside 5.0 - 20.0 with QC passed is clear: " + row.getNormalRange() + " / "
                        + row.getQcStatus() + " / normal=" + row.isNormal(), clear);
            } else {
                assertTrue("25.0 above the range is not clear", !clear);
            }
        }
    }

    @Test
    public void bulkRelease_whenTheFlagIsOff_returns403AndReleasesNothing() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.ALLOW_BULK_RELEASE_CLEAR, "false");

        mockMvc.perform(post("/rest/AccessionValidation/release-clear").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(requestBody(rowJson(CLEAR_ID, "", ""))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("bulkReleaseDisabled"));

        assertEquals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance),
                analysisService.get(CLEAR_ID).getStatusId());
    }

    @Test
    public void bulkRelease_releasesOnlyTheServerSideClearRowsAndReportsTheRest() throws Exception {
        mockMvc.perform(
                post("/rest/AccessionValidation/release-clear").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(rowJson(CLEAR_ID, "Batch reviewed", "E"), rowJson(ABNORMAL_ID, "", ""),
                                rowJson("999999", "", ""))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.released.length()").value(1))
                .andExpect(jsonPath("$.released[0]").value(CLEAR_ID)).andExpect(jsonPath("$.skipped.length()").value(2))
                .andExpect(jsonPath("$.skipped[0].analysisId").value(ABNORMAL_ID))
                .andExpect(jsonPath("$.skipped[0].reason").value("notClear"))
                .andExpect(jsonPath("$.skipped[1].analysisId").value("999999"))
                .andExpect(jsonPath("$.skipped[1].reason").value("notFound"));

        Analysis released = analysisService.get(CLEAR_ID);
        assertEquals(statusService.getStatusID(AnalysisStatus.Finalized), released.getStatusId());
        assertNotNull("release must stamp released_date", released.getReleasedDate());

        Analysis abnormal = analysisService.get(ABNORMAL_ID);
        assertEquals("an abnormal row is never released in bulk",
                statusService.getStatusID(AnalysisStatus.TechnicalAcceptance), abnormal.getStatusId());
        assertNull(abnormal.getReleasedDate());

        List<Map<String, Object>> notes = jdbcTemplate.queryForList(
                "SELECT note_type, text FROM clinlims.note WHERE reference_id = ? AND subject = ?",
                Integer.valueOf(CLEAR_ID), "Result Note (Validation)");
        assertEquals(1, notes.size());
        assertEquals("E", notes.get(0).get("note_type"));
        assertEquals("Batch reviewed", notes.get(0).get("text"));
    }

    @Test
    public void bulkRelease_withOnlyNonClearRowsRequested_releasesNothing() throws Exception {
        mockMvc.perform(post("/rest/AccessionValidation/release-clear").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(requestBody(rowJson(ABNORMAL_ID, "", ""))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.released.length()").value(0))
                .andExpect(jsonPath("$.skipped[0].reason").value("notClear"));

        assertEquals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance),
                analysisService.get(ABNORMAL_ID).getStatusId());
        assertEquals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance),
                analysisService.get(CLEAR_ID).getStatusId());
    }

    @Test
    public void bulkRelease_withoutRows_returns400() throws Exception {
        mockMvc.perform(post("/rest/AccessionValidation/release-clear").session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"accessionNumber\":\"" + ACCESSION + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("noRows"));
    }
}
