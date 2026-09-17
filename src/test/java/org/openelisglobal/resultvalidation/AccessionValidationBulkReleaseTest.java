package org.openelisglobal.resultvalidation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.resultvalidation.util.ResultsValidationUtility;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
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
 * OGC-1029 (Validation v4 slice V3) — the guarded bulk release: the server
 * re-derives the Clear lane from its own load of the queue and releases only
 * the requested analyses that are clear, under the "allow bulk release of clear
 * results" flag. Fixture: {@code testdata/validation-bulk-release.xml} —
 * accession VAL-BR-001; analysis 100 is clear (10.5 in 5.0 - 20.0, QC passed),
 * analysis 102 is abnormal (25.0).
 */
@Transactional
public class AccessionValidationBulkReleaseTest extends BaseWebContextSensitiveTest {

    @PersistenceContext
    private EntityManager entityManager;

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
    private UserService userService;

    @Autowired
    private SystemUserService systemUserService;

    private MockHttpSession session;
    private String bulkFlagBefore;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/validation-bulk-release.xml");
        // DbUnit cannot represent the Infinity sentinel for an all-ages limit.
        // Resolve the migration-owned Numeric type; do not recreate missing seeds
        // or reinitialize the production service to match damaged shared state.
        org.openelisglobal.typeoftestresult.valueholder.TypeOfTestResult numeric = typeOfTestResultService
                .getTypeOfTestResultByType("N");
        assertNotNull("Numeric result type must be provided by database migrations", numeric);
        jdbcTemplate.update(
                "UPDATE clinlims.result_limits SET max_age = 'Infinity', test_result_type_id = ? WHERE id = 1",
                Integer.valueOf(numeric.getId()));
        authenticateAs("testUser");
        userService.saveUserLabUnitRoles(systemUserService.get("1"), Map.of("AllLabUnits", Set.of("9400")), "1");
        entityManager.flush();
        entityManager.clear();
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
        entityManager.flush();
        entityManager.clear();

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
        entityManager.flush();
        entityManager.clear();

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
        entityManager.flush();
        entityManager.clear();

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
        entityManager.flush();
        entityManager.clear();
    }
}
