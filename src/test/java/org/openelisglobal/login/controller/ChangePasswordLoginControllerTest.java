package org.openelisglobal.login.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.login.service.LoginUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Integration tests for the change-password endpoint, particularly the
 * apiCall=true JSON contract the React SPA relies on. The legacy JSP
 * forward/redirect responses are indistinguishable from a security bounce to
 * /LoginPage, which made the SPA report success on failed attempts.
 */
public class ChangePasswordLoginControllerTest extends BaseWebContextSensitiveTest {

    private static final String CURRENT_PASSWORD = "adminADMIN!";
    private static final String NEW_PASSWORD = "tempPASS1$";

    @Autowired
    private LoginUserService loginService;

    @Before
    public void loadUsers() throws Exception {
        executeDataSetWithStateManagement("testdata/system-user.xml");
    }

    private MockHttpServletRequestBuilder changePasswordRequest(String currentPassword, String newPassword,
            boolean apiCall) {
        return post("/ChangePasswordLogin" + (apiCall ? "?apiCall=true" : ""))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("loginName", "admin")
                .param("password", currentPassword).param("newPassword", newPassword)
                .param("confirmPassword", newPassword);
    }

    @Test
    public void changePassword_apiCallWrongCurrentPassword_returns401AndLeavesPasswordUnchanged() throws Exception {
        mockMvc.perform(changePasswordRequest("wrongPASS9!", NEW_PASSWORD, true)).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("login.error.password.current.incorrect"));

        Assert.assertTrue("original password must still be valid",
                loginService.getValidatedLogin("admin", CURRENT_PASSWORD).isPresent());
        Assert.assertFalse("rejected new password must not authenticate",
                loginService.getValidatedLogin("admin", NEW_PASSWORD).isPresent());
    }

    @Test
    public void changePassword_apiCallCorrectCurrentPassword_returns200AndChangesPassword() throws Exception {
        mockMvc.perform(changePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD, true)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true));

        Assert.assertTrue("new password must authenticate after change",
                loginService.getValidatedLogin("admin", NEW_PASSWORD).isPresent());
        Assert.assertFalse("old password must no longer authenticate",
                loginService.getValidatedLogin("admin", CURRENT_PASSWORD).isPresent());
    }

    @Test
    public void changePassword_apiCallMismatchedConfirmPassword_returns401AndLeavesPasswordUnchanged()
            throws Exception {
        mockMvc.perform(post("/ChangePasswordLogin?apiCall=true").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("loginName", "admin").param("password", CURRENT_PASSWORD).param("newPassword", NEW_PASSWORD)
                .param("confirmPassword", "otherPASS2$")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("login.error.password.notmatch"));

        Assert.assertTrue("original password must still be valid",
                loginService.getValidatedLogin("admin", CURRENT_PASSWORD).isPresent());
    }

    @Test
    public void changePassword_legacyWrongCurrentPassword_forwardsToJspWithoutRedirect() throws Exception {
        mockMvc.perform(changePasswordRequest("wrongPASS9!", NEW_PASSWORD, false)).andExpect(status().isOk())
                .andExpect(forwardedUrl("/pages/common/formTemplate.jsp"));

        Assert.assertTrue("original password must still be valid",
                loginService.getValidatedLogin("admin", CURRENT_PASSWORD).isPresent());
    }

    @Test
    public void changePassword_legacyCorrectCurrentPassword_redirectsToLoginPage() throws Exception {
        mockMvc.perform(changePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD, false))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/LoginPage"));

        Assert.assertTrue("new password must authenticate after change",
                loginService.getValidatedLogin("admin", NEW_PASSWORD).isPresent());
    }
}
