package org.openelisglobal.systemuser.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MvcResult;

/**
 * OGC-1545: two people with the same name can both have accounts, since the
 * login is what identifies a user; and a save that writes nothing answers 400
 * naming why, where it used to answer 200. Fixture:
 * {@code testdata/unified-system-user-lab-units.xml} — John Doe with login
 * jdoe.
 */
public class UnifiedSystemUserSaveRefusalTest extends BaseWebContextSensitiveTest {

    private static final String PASSWORD = "Ogc1545Strong!pass";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockHttpSession session;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/unified-system-user-lab-units.xml");
        session = buildAdminSession();
    }

    @Test
    public void aSecondUserWithTheSameNameAndAnotherLoginIsCreated() throws Exception {
        MvcResult saved = mockMvc
                .perform(post("/rest/UnifiedSystemUser").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser("jdoe2", "John", "Doe"))))
                .andReturn();

        assertEquals(saved.getResponse().getContentAsString(), 200, saved.getResponse().getStatus());

        assertEquals(Integer.valueOf(2),
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM clinlims.system_user WHERE first_name = 'John' AND last_name = 'Doe'",
                        Integer.class));
        assertEquals(Integer.valueOf(1), jdbcTemplate
                .queryForObject("SELECT count(*) FROM clinlims.login_user WHERE login_name = 'jdoe2'", Integer.class));
    }

    @Test
    public void aLoginAlreadyInUseIsRefusedWith400NamingWhyAndWritesNothing() throws Exception {
        mockMvc.perform(post("/rest/UnifiedSystemUser").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser("jdoe", "Jane", "Roe"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorKey").value("errors.loginName.duplicated"))
                .andExpect(jsonPath("$.error").value(CoreMatchers.containsString("jdoe")));

        assertEquals(Integer.valueOf(0),
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM clinlims.system_user WHERE first_name = 'Jane' AND last_name = 'Roe'",
                        Integer.class));
    }

    /**
     * A login or name longer than its 30-character column used to fail at insert
     * and answer a bare 500.
     */
    @Test
    public void aLoginLongerThanItsColumnIsRefusedWith400NamingTheLimit() throws Exception {
        String login = "j".repeat(31);

        mockMvc.perform(post("/rest/UnifiedSystemUser").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser(login, "Jane", "Roe"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorKey").value("errors.maxlength"))
                .andExpect(jsonPath("$.error").value(CoreMatchers.containsString("30")));

        assertEquals(Integer.valueOf(0), jdbcTemplate
                .queryForObject("SELECT count(*) FROM clinlims.login_user WHERE login_name = ?", Integer.class, login));
    }

    /**
     * Two saves of the same new login at once both passed the check before either
     * was written, so the login got two rows: it could no longer sign in and Modify
     * User could not open it.
     */
    @Test
    public void twoSavesOfTheSameNewLoginAtOnceCreateOneAccount() throws Exception {
        String body = objectMapper.writeValueAsString(newUser("jtwin", "Jane", "Twin"));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> saves = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                saves.add(pool.submit(() -> {
                    start.await();
                    return mockMvc
                            .perform(post("/rest/UnifiedSystemUser").session(session)
                                    .contentType(MediaType.APPLICATION_JSON).content(body))
                            .andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> save : saves) {
                statuses.add(save.get(60, TimeUnit.SECONDS));
            }
            Collections.sort(statuses);
            assertEquals(List.of(200, 400), statuses);
        } finally {
            pool.shutdownNow();
        }

        assertEquals(Integer.valueOf(1), jdbcTemplate
                .queryForObject("SELECT count(*) FROM clinlims.login_user WHERE login_name = 'jtwin'", Integer.class));
    }

    private Map<String, Object> newUser(String login, String firstName, String lastName) throws Exception {
        String json = mockMvc
                .perform(get("/rest/UnifiedSystemUser").session(session).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Map<String, Object> form = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
        form.keySet().removeIf(key -> List.of("globalRoles", "labUnitRoles", "testSections").contains(key));
        form.put("userLoginName", login);
        form.put("userFirstName", firstName);
        form.put("userLastName", lastName);
        form.put("userPassword", PASSWORD);
        form.put("confirmPassword", PASSWORD);
        form.put("timeout", "30");
        form.put("allowCopyUserRoles", "N");
        return form;
    }

    private MockHttpSession buildAdminSession() {
        UserDetails userDetails = User.withUsername("admin").password("N/A").authorities("ROLE_ADMIN").build();
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(1);
        sessionData.setAdmin(true);
        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return httpSession;
    }
}
