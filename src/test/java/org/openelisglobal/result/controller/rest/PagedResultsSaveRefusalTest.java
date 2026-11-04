package org.openelisglobal.result.controller.rest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * The Results and Validation saves apply the posted rows to the page this
 * session loaded. A post sent before any page was loaded, or naming no loaded
 * page, answers 400 with the reason instead of a bare 500.
 */
public class PagedResultsSaveRefusalTest extends BaseWebContextSensitiveTest {

    private MockHttpSession session;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        session = authenticatedSession();
    }

    @Test
    public void aResultsSaveBeforeAnyPageWasLoadedIsRefusedSayingSo() throws Exception {
        mockMvc.perform(post("/rest/LogbookResults").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"testResult\":[]}")).andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("load")));
    }

    @Test
    public void aValidationSaveBeforeAnyPageWasLoadedIsRefusedSayingSo() throws Exception {
        mockMvc.perform(post("/rest/AccessionValidation").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"resultList\":[]}")).andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("load")));
    }

    @Test
    public void aResultsSaveWithoutPagingIsRefusedNamingTheField() throws Exception {
        loadOneEmptyPage();

        mockMvc.perform(post("/rest/LogbookResults").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"testResult\":[]}")).andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("paging.currentPage")));
    }

    @Test
    public void aValidationSaveNamingAPageThatWasNotLoadedIsRefusedNamingIt() throws Exception {
        loadOnePageHolding(new AnalysisItem());

        mockMvc.perform(post("/rest/AccessionValidation").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"resultList\":[],\"paging\":{\"currentPage\":\"9\",\"totalPages\":\"1\"}}"))
                .andExpect(status().isBadRequest()).andExpect(status().reason(containsString(": 9")));
    }

    @Test
    public void aResultsSaveOfTheLoadedPageIsAccepted() throws Exception {
        loadOneEmptyPage();

        mockMvc.perform(post("/rest/LogbookResults").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"testResult\":[],\"paging\":{\"currentPage\":\"1\",\"totalPages\":\"1\"}}"))
                .andExpect(status().isOk());
    }

    private void loadOneEmptyPage() {
        loadOnePageHolding();
    }

    private void loadOnePageHolding(Object... rows) {
        List<List<Object>> pages = new ArrayList<>();
        pages.add(new ArrayList<>(List.of(rows)));
        session.setAttribute(IActionConstants.RESULTS_SESSION_CACHE, pages);
    }

    private MockHttpSession authenticatedSession() {
        UserDetails userDetails = User.withUsername("admin").password("N/A")
                .authorities("ROLE_ADMIN", "ROLE_RESULTS", "ROLE_VALIDATION").build();
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
