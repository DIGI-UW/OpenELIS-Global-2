package org.openelisglobal.resultvalidation;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
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

@Transactional
public class AccessionValidationComponentSelectionsTest extends BaseWebContextSensitiveTest {

    private static final String ACCESSION = "VAL-MS-001";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private ResultsValidationUtility validationUtility;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private UserService userService;

    @Autowired
    private SystemUserService systemUserService;

    private MockHttpSession session;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/component-selections.xml");
        jdbcTemplate.update("INSERT INTO clinlims.status_of_sample (id, description, code, status_type, name,"
                + " is_active, lastupdated) VALUES (9406, 'Finalized', '6', 'ANALYSIS', 'Finalized', 'Y', NOW()),"
                + " (9407, 'Testing finished', '3', 'ORDER', 'Testing finished', 'Y', NOW())");
        statusService.refreshCache();
        authenticateAs("testUser");
        userService.saveUserLabUnitRoles(systemUserService.get("1"), Map.of("AllLabUnits", Set.of("9400")), "1");
        entityManager.flush();
        entityManager.clear();
        session = validatorSession();
    }

    @Test
    public void eachSelectionPartIsItsOwnRowWithItsOwnSelections() {
        Map<String, String> selectionsByRow = validationUtility
                .getValidationAnalysisBySample(sampleService.getSampleByAccessionNumber(ACCESSION)).stream()
                .collect(Collectors.toMap(AnalysisItem::getTestName, AnalysisItem::getMultiSelectResultValues));

        assertEquals(Map.of("Appearance(Swab) — Colour", "{\"1\":\"9301,9303\"}", "Appearance(Swab) — Size",
                "{\"1\":\"9302\"}"), selectionsByRow);
    }

    @Test
    public void validatingAPartKeepsTheSelectionsOfAPrimaryWithNoComponentOnRecord() throws Exception {
        jdbcTemplate.update("UPDATE clinlims.test_result SET component_id = NULL WHERE id IN (1, 3)");
        entityManager.clear();

        release("101");
        release("100");

        assertEquals(List.of("9301", "9302", "9303"), jdbcTemplate.queryForList(
                "SELECT value FROM clinlims.result WHERE analysis_id = 100 ORDER BY value", String.class));
        assertEquals(statusService.getStatusID(AnalysisStatus.Finalized), analysisService.get("100").getStatusId());
    }

    private org.springframework.test.web.servlet.ResultActions release(String resultId) throws Exception {
        JsonNode row = null;
        for (JsonNode candidate : queue()) {
            if (resultId.equals(candidate.path("resultId").asText())) {
                row = candidate;
            }
        }
        org.springframework.test.web.servlet.ResultActions response = mockMvc
                .perform(post("/rest/AccessionValidation/analysis/100/release").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(row.toString()))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();
        return response;
    }

    private JsonNode queue() throws Exception {
        String body = mockMvc
                .perform(get("/rest/AccessionValidation").param("accessionNumber", ACCESSION).session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new ObjectMapper().readTree(body).path("resultList");
    }

    private MockHttpSession validatorSession() {
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
}
