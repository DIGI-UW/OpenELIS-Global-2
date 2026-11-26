package org.openelisglobal.security.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.AppTestConfig;
import org.openelisglobal.BaseTestConfig;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.IsolatedDatabase;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.Errors;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * A SAML or OAuth sign-in used to create an active account for any principal
 * the identity provider named (SCAN-052). The account must exist here unless
 * the installation opts into provisioning. Fixture
 * {@code testdata/system-user.xml}: jdoe exists, nobody named stranger does.
 */
@ContextConfiguration(inheritLocations = false, classes = { AppTestConfig.class, SsoLoginAccountTest.TestConfig.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@IsolatedDatabase
public class SsoLoginAccountTest extends BaseWebContextSensitiveTest {

    /**
     * The test context leaves the security.login package unscanned. The context
     * starts against its own freshly seeded database.
     */
    @Configuration
    public static class TestConfig extends BaseTestConfig {
        private final PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:14.4");

        @Override
        protected PostgreSQLContainer<?> databaseContainer() {
            return database;
        }

        @PreDestroy
        public void stopDatabase() {
            database.stop();
        }

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
    @Autowired
    private LoginUserService loginUserService;

    @Before
    public void loadUsers() throws Exception {
        executeDataSetWithStateManagement("testdata/system-user.xml");
        ReflectionTestUtils.setField(handler, PROVISIONING, false);
    }

    @After
    public void restoreDefault() {
        ReflectionTestUtils.setField(handler, PROVISIONING, false);
        SecurityContextHolder.clearContext();
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

    @Test
    public void inactiveAccountIsRefusedForSamlAndOauth_evenWhenProvisioningIsEnabled() throws Exception {
        ReflectionTestUtils.setField(handler, PROVISIONING, true);
        assertRefused(saml("bwhite"), "login.error.account.disable");
        assertRefused(oauth("bwhite"), "login.error.account.disable");
        assertEquals("N", systemUserService.getMatch("loginName", "bwhite").get().getIsActive());
    }

    @Test
    public void disabledLocalLoginIsRefusedForSamlAndOauth() throws Exception {
        LoginUser login = loginUserService.getMatch("loginName", "jdoe").get();
        login.setAccountDisabled("Y");
        loginUserService.save(login);

        assertRefused(saml("jdoe"), "login.error.account.disable");
        assertRefused(oauth("jdoe"), "login.error.account.disable");
    }

    @Test
    public void lockedLocalLoginIsRefusedForSamlAndOauth() throws Exception {
        LoginUser login = loginUserService.getMatch("loginName", "jdoe").get();
        login.setAccountLocked("Y");
        loginUserService.save(login);

        assertRefused(saml("jdoe"), "login.error.account.lock");
        assertRefused(oauth("jdoe"), "login.error.account.lock");
    }

    @Test
    public void enabledAccountCanUseBothProvidersDespiteExpiredLocalPassword() throws Exception {
        LoginUser login = loginUserService.getMatch("loginName", "jdoe").get();
        login.setPasswordExpiredDate(java.sql.Date.valueOf("2000-01-01"));
        loginUserService.save(login);
        for (Authentication authentication : List.of(saml("jdoe"), oauth("jdoe"))) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            handler.onAuthenticationSuccess(request, response, authentication);
            UserSessionData session = (UserSessionData) request.getSession()
                    .getAttribute(IActionConstants.USER_SESSION_DATA);
            assertNotNull(session);
            assertEquals(3, session.getSystemUserId());
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            assertNull(request.getSession().getAttribute(Constants.LOGIN_ERRORS));
        }
    }

    @Test
    public void activeSsoOnlyAccountDoesNotRequireLocalPasswordCredentials() throws Exception {
        loginUserService.delete(loginUserService.getMatch("loginName", "jdoe").get());
        for (Authentication authentication : List.of(saml("jdoe"), oauth("jdoe"))) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            handler.onAuthenticationSuccess(request, response, authentication);
            assertNotNull(request.getSession().getAttribute(IActionConstants.USER_SESSION_DATA));
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    private void assertRefused(Authentication authentication, String errorCode) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("apiCall", "true");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession previousSession = (MockHttpSession) request.getSession();
        previousSession.setAttribute(IActionConstants.USER_SESSION_DATA, new UserSessionData());
        previousSession.setAttribute(IActionConstants.PERMITTED_ACTIONS_MAP, java.util.Set.of("MasterList"));
        previousSession.setAttribute("authorities", "previous-authorities");
        previousSession.setAttribute("login_method", "samlLogin");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        previousSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals("/LoginPage", response.getRedirectedUrl());
        assertFalse(response.getContentAsString().contains("true"));
        assertTrue(previousSession.isInvalid());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(request.getSession().getAttribute(IActionConstants.USER_SESSION_DATA));
        assertNull(request.getSession().getAttribute(IActionConstants.PERMITTED_ACTIONS_MAP));
        assertNull(request.getSession().getAttribute("authorities"));
        assertNull(request.getSession().getAttribute("login_method"));
        assertNull(request.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        Errors errors = (Errors) request.getSession().getAttribute(Constants.LOGIN_ERRORS);
        assertEquals(errorCode, errors.getAllErrors().get(0).getCode());
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
