package org.openelisglobal.systemUser.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MvcResult;

public class UnifiedSystemUserRestControllerTest extends BaseWebContextSensitiveTest {

    private MockHttpSession session;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/system-user.xml");
        UserDetails userDetails = User.withUsername("admin").password("N/A")
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, "N/A",
                userDetails.getAuthorities());
        SecurityContext sc = new SecurityContextImpl();
        sc.setAuthentication(auth);
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
    }

    @Test
    public void getUsersWithRole_shouldReturnSystemUsersWithRoles() throws Exception {
        MvcResult urlResult = mockMvc.perform(get("/rest/users").accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE).session(session)).andReturn();
        String results = urlResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> users = objectMapper.readValue(results,
                new TypeReference<List<Map<String, Object>>>() {
                });
        assertEquals(4, users.size());

        Map<String, Object> user1 = users.get(1);
        assertEquals("3", user1.get("id"));
        assertEquals("Doe,John", user1.get("value"));

        Map<String, Object> user2 = users.get(2);
        assertEquals("4", user2.get("id"));
        assertEquals("Smith,Alice", user2.get("value"));

        Map<String, Object> user3 = users.get(3);
        assertEquals("5", user3.get("id"));
        assertEquals("White,Bob", user3.get("value"));
    }

    @Test
    public void getUsersWithRole_shouldReturnSystemUsersGivenThereRole() throws Exception {
        MvcResult urlResult = mockMvc.perform(get("/rest/users/adminRole").accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE).session(session)).andReturn();
        String results = urlResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> users = objectMapper.readValue(results,
                new TypeReference<List<Map<String, Object>>>() {
                });
        assertEquals(3, users.size());

        Map<String, Object> user1 = users.get(1);
        assertEquals("3", user1.get("id"));
        assertEquals("Doe,John", user1.get("value"));

        Map<String, Object> user2 = users.get(2);
        assertEquals("4", user2.get("id"));
        assertEquals("Smith,Alice", user2.get("value"));
    }

}