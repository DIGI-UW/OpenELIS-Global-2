package org.openelisglobal.security.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.AppTestConfig;
import org.openelisglobal.BaseTestConfig;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.Errors;

/**
 * A SAML or OAuth sign-in used to create an active account for any principal
 * the identity provider named (SCAN-052). The account must exist here unless
 * the installation opts into provisioning. Fixture
 * {@code testdata/system-user.xml}: jdoe exists, nobody named stranger does.
 */
@ContextConfiguration(inheritLocations = false, classes = { AppTestConfig.class, SsoLoginAccountTest.TestConfig.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SsoLoginAccountTest extends BaseWebContextSensitiveTest {

    /** The test context leaves the security.login package unscanned. */
    @Configuration
    public static class TestConfig extends BaseTestConfig {
        @Bean
        public CustomSSOAuthenticationSuccessHandler ssoSuccessHandler() {
            return new CustomSSOAuthenticationSuccessHandler();
        }
    }

    private static final String PROVISIONING = "provisionUnknownUsers";

    @Autowired
    private CustomSSOAuthenticationSuccessHandler handler;
    @Autowired
    private SystemUserService systemUserService;

    @Before
    public void loadUsers() throws Exception {
        executeDataSetWithStateManagement("testdata/system-user.xml");
        ReflectionTestUtils.setField(handler, PROVISIONING, false);
    }

    @After
    public void restoreDefault() {
        ReflectionTestUtils.setField(handler, PROVISIONING, false);
    }

    @Test
    public void samlPrincipalWithoutAnAccount_isRefusedAndNoAccountIsCreated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, saml("stranger"));

        assertEquals("/LoginPage", response.getRedirectedUrl());
        Errors errors = (Errors) request.getSession().getAttribute(Constants.LOGIN_ERRORS);
        assertNotNull(errors);
        assertEquals("login.error.noOeUser", errors.getAllErrors().get(0).getCode());
        assertNull(request.getSession().getAttribute(IActionConstants.USER_SESSION_DATA));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertFalse(systemUserService.getMatch("loginName", "stranger").isPresent());
    }

    @Test
    public void oauthPrincipalWithoutAnAccount_isRefusedAndNoAccountIsCreated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, oauth("stranger"));

        assertEquals("/LoginPage", response.getRedirectedUrl());
        assertNull(request.getSession().getAttribute(IActionConstants.USER_SESSION_DATA));
        assertFalse(systemUserService.getMatch("loginName", "stranger").isPresent());
    }

    @Test
    public void samlPrincipalWithAnAccount_getsThatAccountsSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, saml("jdoe"));

        assertEquals("/Home", response.getRedirectedUrl());
        UserSessionData session = (UserSessionData) request.getSession()
                .getAttribute(IActionConstants.USER_SESSION_DATA);
        assertEquals(3, session.getSystemUserId());
    }

    @Test
    public void withProvisioningEnabled_unknownPrincipalGetsAnAccount() throws Exception {
        ReflectionTestUtils.setField(handler, PROVISIONING, true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, saml("stranger"));

        assertEquals("/Home", response.getRedirectedUrl());
        assertTrue(systemUserService.getMatch("loginName", "stranger").isPresent());
    }

    private Authentication saml(String name) {
        return new UsernamePasswordAuthenticationToken(new DefaultSaml2AuthenticatedPrincipal(name, Map.of()), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private Authentication oauth(String name) {
        return new UsernamePasswordAuthenticationToken(
                new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")),
                        Map.of("preferred_username", name), "preferred_username"),
                null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
