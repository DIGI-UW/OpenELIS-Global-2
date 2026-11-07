package org.openelisglobal.login.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.security.LoginAttemptTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The password change before login is reachable without a session, so it is a
 * place to guess passwords (INV-3.16). After the configured number of failures
 * the login waits out the window, right password or not.
 */
public class ChangePasswordLoginThrottleTest extends BaseWebContextSensitiveTest {

    private static final String CURRENT_PASSWORD = "adminADMIN!";
    private static final String NEW_PASSWORD = "tempPASS1$";
    private static final int LIMIT = 10;

    @Autowired
    private LoginUserService loginService;
    @Autowired
    private LoginAttemptTracker tracker;

    @Before
    public void loadUsers() throws Exception {
        executeDataSetWithStateManagement("testdata/system-user.xml");
        tracker.clear();
    }

    @After
    public void forgetAttempts() {
        tracker.clear();
    }

    @Test
    public void afterTheLimit_theRightPasswordIsRefusedAsThrottled() throws Exception {
        for (int i = 0; i < LIMIT; i++) {
            mockMvc.perform(changePassword("admin", "wrongPASS9!")).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("login.error.password.current.incorrect"));
        }

        mockMvc.perform(changePassword("admin", CURRENT_PASSWORD)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("login.error.throttled"));

        Assert.assertTrue(loginService.getValidatedLogin("admin", CURRENT_PASSWORD).isPresent());
    }

    @Test
    public void belowTheLimit_theRightPasswordStillChanges() throws Exception {
        for (int i = 0; i < LIMIT - 1; i++) {
            mockMvc.perform(changePassword("admin", "wrongPASS9!")).andExpect(status().isUnauthorized());
        }

        mockMvc.perform(changePassword("admin", CURRENT_PASSWORD)).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Assert.assertTrue(loginService.getValidatedLogin("admin", NEW_PASSWORD).isPresent());
    }

    private MockHttpServletRequestBuilder changePassword(String loginName, String currentPassword) {
        return post("/ChangePasswordLogin?apiCall=true").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("loginName", loginName).param("password", currentPassword).param("newPassword", NEW_PASSWORD)
                .param("confirmPassword", NEW_PASSWORD);
    }
}
