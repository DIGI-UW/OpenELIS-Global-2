package org.openelisglobal.login.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
 * The password change before login used to verify the current password before
 * looking at the account's lock, and to name each case in its reply, so a
 * locked or disabled account confirmed its password to anyone (SCAN-108,
 * GHSA-vpjp-h9jc-vf8p). Fixture
 * {@code testdata/change-password-closed-logins.xml}: lockedjane and
 * disabledjane share admin's password.
 */
public class ChangePasswordLoginClosedAccountTest extends BaseWebContextSensitiveTest {

    private static final String CURRENT_PASSWORD = "adminADMIN!";
    private static final String NEW_PASSWORD = "tempPASS1$";
    private static final String INCORRECT = "login.error.password.current.incorrect";

    @Autowired
    private LoginUserService loginService;

    @Before
    public void loadUsers() throws Exception {
        executeDataSetWithStateManagement("testdata/change-password-closed-logins.xml");
    }

    @Test
    public void lockedAccount_correctPassword_isRefusedAsIncorrectAndUnchanged() throws Exception {
        mockMvc.perform(changePassword("lockedjane", CURRENT_PASSWORD, true)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(INCORRECT));

        Assert.assertTrue(loginService.getValidatedLogin("lockedjane", CURRENT_PASSWORD).isPresent());
        Assert.assertFalse(loginService.getValidatedLogin("lockedjane", NEW_PASSWORD).isPresent());
    }

    @Test
    public void lockedAccount_wrongPassword_getsTheSameReply() throws Exception {
        String withCorrect = mockMvc.perform(changePassword("lockedjane", CURRENT_PASSWORD, true)).andReturn()
                .getResponse().getContentAsString();
        String withWrong = mockMvc.perform(changePassword("lockedjane", "wrongPASS9!", true)).andReturn().getResponse()
                .getContentAsString();

        Assert.assertEquals(withWrong, withCorrect);
    }

    @Test
    public void disabledAccount_correctPassword_isRefusedAsIncorrectAndUnchanged() throws Exception {
        mockMvc.perform(changePassword("disabledjane", CURRENT_PASSWORD, true)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(INCORRECT));

        Assert.assertTrue(loginService.getValidatedLogin("disabledjane", CURRENT_PASSWORD).isPresent());
    }

    @Test
    public void lockedAccount_legacyForm_forwardsLikeAWrongPassword() throws Exception {
        mockMvc.perform(changePassword("lockedjane", CURRENT_PASSWORD, false)).andExpect(status().isOk())
                .andExpect(forwardedUrl("/pages/common/formTemplate.jsp"));

        Assert.assertTrue(loginService.getValidatedLogin("lockedjane", CURRENT_PASSWORD).isPresent());
    }

    @Test
    public void openAccount_correctPassword_stillChanges() throws Exception {
        mockMvc.perform(changePassword("admin", CURRENT_PASSWORD, true)).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Assert.assertTrue(loginService.getValidatedLogin("admin", NEW_PASSWORD).isPresent());
    }

    private MockHttpServletRequestBuilder changePassword(String loginName, String currentPassword, boolean apiCall) {
        return post("/ChangePasswordLogin" + (apiCall ? "?apiCall=true" : ""))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("loginName", loginName)
                .param("password", currentPassword).param("newPassword", NEW_PASSWORD)
                .param("confirmPassword", NEW_PASSWORD);
    }
}
