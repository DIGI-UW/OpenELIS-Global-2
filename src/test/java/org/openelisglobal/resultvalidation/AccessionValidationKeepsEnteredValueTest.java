package org.openelisglobal.resultvalidation;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.action.IActionConstants;
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
 * OGC-1544: a numeric result is shown rounded to its test's decimal places, but
 * the value entered or received is what is stored, and validating it does not
 * replace it. Before, 0.9 on a test with no decimal places showed "0" and
 * releasing the row stored "0". Fixture:
 * {@code testdata/validation-review-panel.xml} — accession VAL-RP-001, result
 * 100 on analysis 100 awaiting validation, here entered as 0.9 on a test
 * reporting no decimals.
 */
@Transactional
public class AccessionValidationKeepsEnteredValueTest extends BaseWebContextSensitiveTest {

    private static final String ACCESSION = "VAL-RP-001";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private UserService userService;

    @Autowired
    private SystemUserService systemUserService;

    private MockHttpSession session;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/validation-review-panel.xml");
        jdbcTemplate.update("UPDATE clinlims.result SET value = '0.9', significant_digits = 0 WHERE id = 100");
        authenticateAs("testUser");
        userService.saveUserLabUnitRoles(systemUserService.get("1"), Map.of("AllLabUnits", Set.of("9400")), "1");
        entityManager.flush();
        entityManager.clear();
        session = validatorSession();
    }

    @Test
    public void validationShowsTheReportedValueAndCarriesTheEnteredOne() throws Exception {
        String body = mockMvc
                .perform(get("/rest/AccessionValidation").param("accessionNumber", ACCESSION).session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        JsonNode row = rowFor(new ObjectMapper().readTree(body), "100");
        assertEquals("1", row.get("result").asText());
        assertEquals("0.9", row.get("storedResult").asText());
    }

    @Test
    public void releasingTheRowAsShownKeepsTheEnteredValue() throws Exception {
        release("1");

        assertEquals("0.9", resultService.get("100").getValue());
    }

    @Test
    public void aValueTheValidatorChangedOnReleaseIsSaved() throws Exception {
        release("2");

        assertEquals("2", resultService.get("100").getValue());
    }

    @Test
    public void modifySavesTheValueAsTyped() throws Exception {
        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/modify").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(row("0.95"))).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        assertEquals("0.95", resultService.get("100").getValue());
    }

    private void release(String shownValue) throws Exception {
        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/release").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(row(shownValue))).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();
    }

    private JsonNode rowFor(JsonNode form, String analysisId) {
        for (JsonNode row : form.get("resultList")) {
            if (analysisId.equals(row.get("analysisId").asText())) {
                return row;
            }
        }
        throw new AssertionError("no validation row for analysis " + analysisId);
    }

    private String row(String value) {
        long token = analysisService.get("100").getLastupdated().getTime();
        return "{\"analysisId\":\"100\",\"accessionNumber\":\"" + ACCESSION + "\",\"resultId\":\"100\","
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
