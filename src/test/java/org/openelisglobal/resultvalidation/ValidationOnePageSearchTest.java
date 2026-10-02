package org.openelisglobal.resultvalidation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultService;
import org.openelisglobal.typeoftestresult.valueholder.TypeOfTestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1418 — Validation's one search: Lab Unit, lab number or range, patient
 * and date range combine, a validator never sees a Lab Unit they cannot
 * validate, and Release all clear covers only the rows of the search on the
 * page. Fixture: {@code testdata/validation-one-page-search.xml}.
 */
@Transactional
public class ValidationOnePageSearchTest extends BaseWebContextSensitiveTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TypeOfTestResultService typeOfTestResultService;
    @Autowired
    private UserService userService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private IStatusService statusService;

    private final ObjectMapper mapper = new ObjectMapper();
    private MockHttpSession session;
    private String bulkFlagBefore;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/validation-one-page-search.xml");
        TypeOfTestResult numeric = typeOfTestResultService.getTypeOfTestResultByType("N");
        assertNotNull("Numeric result type must be provided by database migrations", numeric);
        jdbcTemplate.update("INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id, min_age, max_age,"
                + " low_normal, high_normal, low_valid, high_valid, low_reporting_range, high_reporting_range,"
                + " always_validate, lastupdated) VALUES (9811, 1, ?, 0, 'Infinity', 5, 20, 0, 100, 0, 100, false,"
                + " NOW()), (9812, 2, ?, 0, 'Infinity', 5, 20, 0, 100, 0, 100, false, NOW())"
                + " ON CONFLICT (id) DO NOTHING", Integer.valueOf(numeric.getId()), Integer.valueOf(numeric.getId()));
        jdbcTemplate.update("INSERT INTO clinlims.analysis (id, sampitem_id, vector_pool_id, test_id, test_sect_id,"
                + " revision, status_id, started_date, analysis_type, is_reportable, lastupdated) VALUES (205, NULL,"
                + " 205, 1, 1, 0, 2, '2025-06-20 11:00:00', 'MANUAL', 'Y', '2025-06-20 12:00:00')");
        jdbcTemplate.update("INSERT INTO clinlims.result (id, analysis_id, result_type, value, is_reportable,"
                + " lastupdated) VALUES (205, 205, 'N', '10.5', 'Y', '2025-06-20 12:00:00')");
        authenticateAs("testUser");
        statusService.refreshCache();
        session = buildValidatorSession();
        bulkFlagBefore = ConfigurationProperties.getInstance().getPropertyValue(Property.ALLOW_BULK_RELEASE_CLEAR);
        ConfigurationProperties.getInstance().setPropertyValue(Property.ALLOW_BULK_RELEASE_CLEAR, "true");
    }

    @After
    public void restoreConfiguration() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.ALLOW_BULK_RELEASE_CLEAR,
                bulkFlagBefore == null ? "true" : bulkFlagBefore);
    }

    private boolean granted;

    private void grantValidation(String labUnit) {
        granted = true;
        userService.saveUserLabUnitRoles(systemUserService.get("1"), Map.of(labUnit, Set.of("9400")), "1");
        entityManager.flush();
        entityManager.clear();
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

    private static String day(String isoDate) {
        return DateUtil.formatDateAsText(java.sql.Date.valueOf(isoDate));
    }

    /** The lab numbers the queue holds for a search, sorted. */
    private Set<String> queue(String... params) throws Exception {
        if (!granted) {
            grantValidation("AllLabUnits");
        }
        MockHttpServletRequestBuilder request = get("/rest/AccessionValidation").session(session);
        for (int i = 0; i < params.length; i += 2) {
            request = request.param(params[i], params[i + 1]);
        }
        String body = mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        Set<String> accessions = new TreeSet<>();
        for (JsonNode row : mapper.readTree(body).path("resultList")) {
            accessions.add(row.path("accessionNumber").asText());
        }
        return accessions;
    }

    @Test
    public void aLabUnitAloneServesThatUnitsQueue() throws Exception {
        assertEquals(Set.of("VAL-OP-001", "VAL-OP-003", "VAL-OP-005"), queue("testSectionId", "1"));
    }

    @Test
    public void labUnitAndDateRangeNarrowEachOther() throws Exception {
        assertEquals(Set.of("VAL-OP-001"),
                queue("testSectionId", "1", "fromDate", day("2025-06-01"), "toDate", day("2025-06-05")));
        assertEquals("the end day is included", Set.of("VAL-OP-003", "VAL-OP-004"),
                queue("fromDate", day("2025-06-10"), "toDate", day("2025-06-10")));
    }

    @Test
    public void aLabNumberRangeCombinesWithTheLabUnit() throws Exception {
        assertEquals(Set.of("VAL-OP-002", "VAL-OP-003", "VAL-OP-004"),
                queue("labNumberFrom", "VAL-OP-002", "labNumberTo", "VAL-OP-004"));
        assertEquals(Set.of("VAL-OP-002", "VAL-OP-004"),
                queue("labNumberFrom", "VAL-OP-002", "labNumberTo", "VAL-OP-004", "testSectionId", "2"));
        assertEquals("a range with no end is every lab number from its start on",
                Set.of("VAL-OP-003", "VAL-OP-004", "VAL-OP-005"), queue("labNumberFrom", "VAL-OP-003"));
    }

    @Test
    public void aLabUnitKeepsTheVectorPoolResultsTheOtherCriteriaFind() throws Exception {
        assertEquals(Set.of("VAL-OP-005"), queue("fromDate", day("2025-06-20"), "toDate", day("2025-06-20")));
        assertEquals(Set.of("VAL-OP-005"),
                queue("testSectionId", "1", "fromDate", day("2025-06-20"), "toDate", day("2025-06-20")));
    }

    @Test
    public void oneLabNumberServesThatOrder() throws Exception {
        assertEquals(Set.of("VAL-OP-003"), queue("labNumberFrom", "VAL-OP-003", "labNumberTo", "VAL-OP-003"));
    }

    @Test
    public void aPatientCombinesWithTheOtherCriteria() throws Exception {
        assertEquals(Set.of("VAL-OP-001", "VAL-OP-002"), queue("patientId", "901"));
        assertEquals(Set.of("VAL-OP-002"), queue("patientId", "901", "testSectionId", "2"));
        assertEquals(Set.of(), queue("patientId", "902", "fromDate", day("2025-06-01"), "toDate", day("2025-06-05")));
    }

    @Test
    public void aValidatorNeverSeesALabUnitTheyCannotValidate() throws Exception {
        grantValidation("1");
        assertEquals(Set.of(), queue("testSectionId", "2"));
        assertEquals(Set.of("VAL-OP-001", "VAL-OP-003", "VAL-OP-005"), queue("fromDate", day("2025-06-01")));
    }

    @Test
    public void theOldSingleKeySearchesStillServeTheirQueues() throws Exception {
        assertEquals(Set.of("VAL-OP-002", "VAL-OP-004"), queue("unitType", "2"));
        assertEquals(Set.of("VAL-OP-002"), queue("accessionNumber", "VAL-OP-002", "doRange", "false"));
    }

    @Test
    public void releaseAllClearForALabUnitAloneReleasesItsVectorPoolResults() throws Exception {
        grantValidation("AllLabUnits");
        String body = "{\"testSectionId\":\"1\",\"doRange\":true,\"rows\":["
                + "{\"analysisId\":\"205\",\"accessionNumber\":\"VAL-OP-005\",\"noteContext\":\"VALIDATION\"}]}";

        mockMvc.perform(post("/rest/AccessionValidation/release-clear").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
                .andExpect(jsonPath("$.released.length()").value(1)).andExpect(jsonPath("$.released[0]").value("205"));
        entityManager.flush();
        entityManager.clear();

        assertEquals(statusService.getStatusID(AnalysisStatus.Finalized), analysisService.get("205").getStatusId());
    }

    @Test
    public void releaseAllClearCoversOnlyTheRowsOfTheSearchOnThePage() throws Exception {
        grantValidation("AllLabUnits");
        String body = "{\"testSectionId\":\"1\",\"fromDate\":\"" + day("2025-06-01") + "\",\"toDate\":\""
                + day("2025-06-05") + "\",\"doRange\":true,\"rows\":["
                + "{\"analysisId\":\"201\",\"accessionNumber\":\"VAL-OP-001\",\"noteContext\":\"VALIDATION\"},"
                + "{\"analysisId\":\"203\",\"accessionNumber\":\"VAL-OP-003\",\"noteContext\":\"VALIDATION\"}]}";

        mockMvc.perform(post("/rest/AccessionValidation/release-clear").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
                .andExpect(jsonPath("$.released.length()").value(1)).andExpect(jsonPath("$.released[0]").value("201"))
                .andExpect(jsonPath("$.skipped[0].analysisId").value("203"))
                .andExpect(jsonPath("$.skipped[0].reason").value("notFound"));
        entityManager.flush();
        entityManager.clear();

        List<String> statuses = new ArrayList<>();
        statuses.add(analysisService.get("201").getStatusId());
        statuses.add(analysisService.get("203").getStatusId());
        assertEquals(List.of(statusService.getStatusID(AnalysisStatus.Finalized),
                statusService.getStatusID(AnalysisStatus.TechnicalAcceptance)), statuses);
    }
}
