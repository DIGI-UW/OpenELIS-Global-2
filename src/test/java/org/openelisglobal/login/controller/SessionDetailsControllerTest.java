package org.openelisglobal.login.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.bean.UserSession;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.DefaultCsrfToken;

/**
 * GET /session must never return the session identifier. The JSESSIONID cookie
 * is HttpOnly so page scripts cannot read it; putting the same value in a JSON
 * body hands it to any script on the page (OGC-1377).
 *
 * <p>
 * The shared test context leaves out the login controllers and Spring
 * Security's MVC argument resolvers, so the test builds the real controller
 * with its real services and serializes its answer the way the endpoint does.
 */
public class SessionDetailsControllerTest extends BaseWebContextSensitiveTest {

    private static final String SESSION_ID = "8F3C1A2B9D0E4F5A6B7C8D9E0F1A2B3C";

    private LoginPageController controller;

    @Before
    public void createController() {
        controller = webApplicationContext.getAutowireCapableBeanFactory().createBean(LoginPageController.class);
    }

    private JsonNode sessionJson(MockHttpSession httpSession) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/session");
        request.setSession(httpSession);
        UserSession body = controller.getSesssionDetails(request,
                new DefaultCsrfToken("X-CSRF-Token", "_csrf", "csrf-value"));
        return new ObjectMapper().readTree(mapToJson(body));
    }

    @Test
    public void anonymousSession_doesNotExposeSessionId() throws Exception {
        MockHttpSession httpSession = new MockHttpSession(null, SESSION_ID);

        JsonNode json = sessionJson(httpSession);

        assertFalse(json.get("authenticated").asBoolean());
        assertFalse(json.has("sessionId"));
        assertFalse(json.toString().contains(httpSession.getId()));
    }

    @Test
    public void authenticatedSession_returnsUserButNotSessionId() throws Exception {
        Integer adminId = jdbcTemplate.queryForObject("SELECT id FROM system_user WHERE login_name = 'admin'",
                Integer.class);
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(adminId);
        MockHttpSession httpSession = new MockHttpSession(null, SESSION_ID);
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        JsonNode json = sessionJson(httpSession);

        assertEquals(true, json.get("authenticated").asBoolean());
        assertEquals("admin", json.get("loginName").asText());
        assertEquals(String.valueOf(adminId), json.get("userId").asText());
        assertEquals("csrf-value", json.get("csrf").asText());
        assertFalse(json.has("sessionId"));
        assertFalse(json.toString().contains(httpSession.getId()));
    }
}
