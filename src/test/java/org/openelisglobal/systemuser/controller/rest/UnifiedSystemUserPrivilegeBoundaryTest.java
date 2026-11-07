package org.openelisglobal.systemuser.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * A User Account Administrator administers accounts but may neither grant the
 * Global Administrator role nor change an account that holds it or the reserved
 * admin login (SCAN-020, SCAN-021). Fixture
 * {@code testdata/unified-system-user-privilege.xml}: jdoe (3-6) is a User
 * Account Administrator, asmith (4-7) and admin (1-1) are Global
 * Administrators, bwhite (5-8) holds no global role.
 */
public class UnifiedSystemUserPrivilegeBoundaryTest extends BaseWebContextSensitiveTest {

    private static final String GLOBAL_ADMINISTRATOR = "1";
    private static final String USER_ACCOUNT_ADMINISTRATOR = "2";
    private static final String BWHITE = "5";
    private static final String BWHITE_FORM = "5-8";

    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private LoginUserService loginService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockHttpSession userAccountAdmin;
    private MockHttpSession globalAdmin;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/unified-system-user-privilege.xml");
        userAccountAdmin = sessionFor("jdoe", 3);
        globalAdmin = sessionFor("asmith", 4);
    }

    @Test
    public void userAccountAdministrator_cannotGrantGlobalAdministrator() throws Exception {
        Map<String, Object> form = readForm(userAccountAdmin, BWHITE_FORM);
        form.put("selectedRoles", List.of(GLOBAL_ADMINISTRATOR));

        save(userAccountAdmin, form).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorKey").value(UnifiedSystemUserRestController.ERROR_GLOBAL_ADMIN_GRANT));

        assertFalse(userRoleService.getRoleIdsForUser(BWHITE).contains(GLOBAL_ADMINISTRATOR));
    }

    @Test
    public void userAccountAdministrator_cannotCopyTheRolesOfAGlobalAdministrator() throws Exception {
        Map<String, Object> form = readForm(userAccountAdmin, BWHITE_FORM);
        form.put("allowCopyUserRoles", IActionConstants.YES);
        form.put("systemUserIdToCopy", "4");

        save(userAccountAdmin, form).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorKey").value(UnifiedSystemUserRestController.ERROR_GLOBAL_ADMIN_GRANT));

        assertFalse(userRoleService.getRoleIdsForUser(BWHITE).contains(GLOBAL_ADMINISTRATOR));
    }

    @Test
    public void userAccountAdministrator_cannotLockAGlobalAdministrator() throws Exception {
        Map<String, Object> form = readForm(userAccountAdmin, "4-7");
        form.put("selectedRoles", List.of());
        form.put("accountLocked", IActionConstants.YES);

        save(userAccountAdmin, form).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorKey").value(UnifiedSystemUserRestController.ERROR_GLOBAL_ADMIN_EDIT));

        assertEquals(IActionConstants.NO, loginService.get(7).getAccountLocked());
        assertTrue(userRoleService.getRoleIdsForUser("4").contains(GLOBAL_ADMINISTRATOR));
    }

    @Test
    public void userAccountAdministrator_cannotResetTheAdminAccount() throws Exception {
        Map<String, Object> form = readForm(userAccountAdmin, "1-1");
        form.put("selectedRoles", List.of());
        form.put("userPassword", "Boundary1!pass");
        form.put("confirmPassword", "Boundary1!pass");

        save(userAccountAdmin, form).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorKey").value(UnifiedSystemUserRestController.ERROR_GLOBAL_ADMIN_EDIT));

        assertTrue(loginService.getValidatedLogin("admin", "adminADMIN!").isPresent());
    }

    @Test
    public void userAccountAdministrator_stillAdministersOrdinaryAccounts() throws Exception {
        Map<String, Object> form = readForm(userAccountAdmin, BWHITE_FORM);
        form.put("selectedRoles", List.of(USER_ACCOUNT_ADMINISTRATOR));
        form.put("accountLocked", IActionConstants.YES);

        save(userAccountAdmin, form).andExpect(status().isOk());

        assertTrue(userRoleService.getRoleIdsForUser(BWHITE).contains(USER_ACCOUNT_ADMINISTRATOR));
        assertEquals(IActionConstants.YES, loginService.get(8).getAccountLocked());
    }

    @Test
    public void globalAdministrator_grantsGlobalAdministrator() throws Exception {
        Map<String, Object> form = readForm(globalAdmin, BWHITE_FORM);
        form.put("selectedRoles", List.of(GLOBAL_ADMINISTRATOR));

        save(globalAdmin, form).andExpect(status().isOk());

        assertTrue(userRoleService.getRoleIdsForUser(BWHITE).contains(GLOBAL_ADMINISTRATOR));
    }

    private org.springframework.test.web.servlet.ResultActions save(MockHttpSession session, Map<String, Object> form)
            throws Exception {
        return mockMvc.perform(post("/rest/UnifiedSystemUser").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)));
    }

    private Map<String, Object> readForm(MockHttpSession session, String combinedId) throws Exception {
        String json = mockMvc
                .perform(get("/rest/UnifiedSystemUser").param("ID", combinedId).session(session)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Map<String, Object> form = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
        form.keySet().removeIf(key -> List.of("globalRoles", "labUnitRoles", "testSections").contains(key));
        return form;
    }

    private MockHttpSession sessionFor(String loginName, int systemUserId) {
        UserDetails userDetails = User.withUsername(loginName).password("N/A").authorities("ROLE_ADMIN").build();
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(systemUserId);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        session.setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return session;
    }
}
