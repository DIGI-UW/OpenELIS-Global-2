package org.openelisglobal.result.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

public class AccessionResultsRestControllerTest extends BaseWebContextSensitiveTest {

    private MockHttpSession session;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/result.xml");
        session = buildAuthenticatedSession();
    }

    private MockHttpSession buildAuthenticatedSession() {
        UserDetails userDetails = User.withUsername("admin").password("N/A").authorities("ROLE_ADMIN", "ROLE_RESULTS")
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

    @Test
    public void getAccessionResults_ShouldResolveSampleAndPatient_WhenAccessionExists() throws Exception {
        mockMvc.perform(get("/rest/accession-results").param("accessionNumber", "13333").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessionNumber").value("13333"))
                .andExpect(jsonPath("$.searchFinished").value(true)).andExpect(jsonPath("$.testResult").isArray())
                .andExpect(jsonPath("$.firstName").value("John")).andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    public void getAccessionResults_ShouldReturnNotFoundError_WhenAccessionDoesNotExist() throws Exception {
        mockMvc.perform(get("/rest/accession-results").param("accessionNumber", "DOES_NOT_EXIST").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessionNumber").value("DOES_NOT_EXIST"))
                .andExpect(jsonPath("$.searchFinished").value(false))
                .andExpect(jsonPath("$.error").value("sample.edit.sample.notFound"))
                .andExpect(jsonPath("$.testResult").isArray());
    }
}
