package org.openelisglobal.resultvalidation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.result.service.ResultService;
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
 * Validating one part of a multi-part result validates that part only. The test
 * stays awaiting validation, and the other part stays in the queue, until its
 * last part with a result is validated; then the test is released. Fixture:
 * {@code testdata/component-units.xml} — accession VAL-CU-001, analysis 100, a
 * viral load (result 100) and its Ct (result 101).
 */
@Transactional
public class AccessionValidationComponentReleaseTest extends BaseWebContextSensitiveTest {

    private static final String ACCESSION = "VAL-CU-001";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

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
        executeDataSetWithStateManagement("testdata/component-units.xml");
        // The fixture carries only the statuses it reads; releasing the last part
        // finalizes the test and finishes the order.
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
    public void validatingOnePartLeavesTheTestAndTheOtherPartAwaitingValidation() throws Exception {
        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/release").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(row("100", "1010"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("accepted"));
        entityManager.flush();
        entityManager.clear();

        assertEquals("the test is not released while its Ct waits",
                statusService.getStatusID(AnalysisStatus.TechnicalAcceptance),
                analysisService.get("100").getStatusId());
        assertNotNull("the viral load is validated", resultService.get("100").getValidatedDate());
        assertNull("the Ct is not", resultService.get("101").getValidatedDate());
        assertEquals("only the Ct is left to validate", List.of("101"), queueResultIds());
    }

    @Test
    public void validatingTheLastPartReleasesTheTest() throws Exception {
        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/release").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(row("100", "1010"))).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/release").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(row("101", "33.0"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("released"));
        entityManager.flush();
        entityManager.clear();

        assertEquals(statusService.getStatusID(AnalysisStatus.Finalized), analysisService.get("100").getStatusId());
        assertEquals(List.of(), queueResultIds());
    }

    @Test
    public void releasingTheTestClearsEveryPartsMarkSoACorrectionReopensThemAll() throws Exception {
        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/release").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(row("100", "1010"))).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/release").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(row("101", "33.0"))).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        assertNull("the part validated first is no longer marked", resultService.get("100").getValidatedDate());
        assertNull(resultService.get("101").getValidatedDate());

        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = ? WHERE id = 100",
                Integer.valueOf(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance)));
        assertEquals("a correction re-opens both parts", List.of("100", "101"), queueResultIds());
    }

    @Test
    public void bothPartsAcceptedInOneSaveReleaseTheTest() throws Exception {
        String queue = mockMvc
                .perform(get("/rest/AccessionValidation").param("accessionNumber", ACCESSION).session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ArrayNode rows = (com.fasterxml.jackson.databind.node.ArrayNode) mapper
                .readTree(queue).path("resultList");
        for (JsonNode row : rows) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) row).put("isAccepted", true);
        }

        mockMvc.perform(post("/rest/AccessionValidation").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"resultList\":" + rows + ",\"paging\":{\"currentPage\":\"1\"}}"))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        assertEquals(statusService.getStatusID(AnalysisStatus.Finalized), analysisService.get("100").getStatusId());
        assertNull(resultService.get("100").getValidatedDate());
        assertNull(resultService.get("101").getValidatedDate());
    }

    @Test
    public void reEnteringAValidatedPartSendsItBackToTheQueue() throws Exception {
        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/release").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(row("100", "1010"))).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/modify").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(row("100", "2020"))).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        assertNull("a changed value is validated again", resultService.get("100").getValidatedDate());
        assertEquals(List.of("100", "101"), queueResultIds());
    }

    /** The rows the Validation page is served for the accession. */
    private List<String> queueResultIds() throws Exception {
        String body = mockMvc
                .perform(get("/rest/AccessionValidation").param("accessionNumber", ACCESSION).session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> ids = new ArrayList<>();
        for (JsonNode row : new ObjectMapper().readTree(body).path("resultList")) {
            ids.add(row.path("resultId").asText());
        }
        Collections.sort(ids);
        return ids;
    }

    private String row(String resultId, String value) {
        long token = analysisService.get("100").getLastupdated().getTime();
        return "{\"analysisId\":\"100\",\"accessionNumber\":\"" + ACCESSION + "\",\"resultId\":\"" + resultId + "\","
                + "\"testId\":\"1\",\"resultType\":\"N\",\"result\":\"" + value + "\",\"note\":\"\","
                + "\"noteVisibility\":\"I\",\"noteContext\":\"VALIDATION\",\"analysisLastupdated\":\"" + token + "\"}";
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
