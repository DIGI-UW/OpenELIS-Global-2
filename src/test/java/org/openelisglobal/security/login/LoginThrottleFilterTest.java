package org.openelisglobal.security.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.security.LoginAttemptTracker;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Form sign-in had no limit on wrong passwords (INV-3.16). The throttle filter
 * sits in front of the password check and answers through the same failure
 * handler as a wrong password. The slice replicates the form login wiring of
 * SecurityConfig with a limit of three.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { LoginThrottleFilterTest.TestConfig.class })
public class LoginThrottleFilterTest extends SecuritySliceMockMvcTest {

    private static final int LIMIT = 3;

    @Autowired
    private LoginAttemptTracker tracker;

    @Before
    public void forgetAttempts() {
        tracker.clear();
    }

    @Test
    public void afterTheLimit_theRightPasswordIsRefusedWithoutSigningIn() throws Exception {
        for (int i = 0; i < LIMIT; i++) {
            MvcResult failed = login("admin", "wrong-password", false);
            assertEquals("/LoginPage", failed.getResponse().getRedirectedUrl());
        }

        MvcResult throttled = login("admin", "password", false);

        assertEquals("/LoginPage", throttled.getResponse().getRedirectedUrl());
        Errors errors = (Errors) throttled.getRequest().getSession().getAttribute(Constants.LOGIN_ERRORS);
        assertNotNull(errors);
        assertEquals("login.error.throttled", errors.getAllErrors().get(0).getCode());
        assertNull(throttled.getRequest().getSession().getAttribute("SPRING_SECURITY_CONTEXT"));
    }

    @Test
    public void afterTheLimit_theApiReplyNamesTheThrottle() throws Exception {
        for (int i = 0; i < LIMIT; i++) {
            login("admin", "wrong-password", true);
        }

        MvcResult throttled = login("admin", "password", true);

        assertEquals(401, throttled.getResponse().getStatus());
        assertEquals("{\"error\":\"error.throttled\"}", throttled.getResponse().getContentAsString());
    }

    @Test
    public void anEncodedLoginPath_isThrottledTheSame() throws Exception {
        for (int i = 0; i < LIMIT; i++) {
            login("admin", "wrong-password", false);
        }

        MvcResult throttled = mockMvc.perform(post(URI.create("/Validate%4Cogin")).param("loginName", "admin")
                .param("password", "password").session(new MockHttpSession())).andReturn();

        assertNotEquals("/", throttled.getResponse().getRedirectedUrl());
        assertNull(throttled.getRequest().getSession().getAttribute("SPRING_SECURITY_CONTEXT"));
    }

    @Test
    public void belowTheLimit_theRightPasswordSignsIn() throws Exception {
        for (int i = 0; i < LIMIT - 1; i++) {
            login("admin", "wrong-password", false);
        }

        MvcResult signedIn = login("admin", "password", false);

        assertEquals("/", signedIn.getResponse().getRedirectedUrl());
        assertNotNull(signedIn.getRequest().getSession().getAttribute("SPRING_SECURITY_CONTEXT"));
    }

    @Test
    public void bundledProxy_keepsTwoClientsSeparate_andIgnoresForgedPrefixes() throws Exception {
        for (int i = 0; i < LIMIT; i++) {
            proxiedLogin("unknown" + i, "wrong-password", "203.0.113." + (i + 1) + ", 192.0.2.10");
        }
        MvcResult blocked = proxiedLogin("admin", "password", "203.0.113.99, 192.0.2.10");
        assertEquals(401, blocked.getResponse().getStatus());
        assertEquals("{\"error\":\"error.throttled\"}", blocked.getResponse().getContentAsString());
        assertNull(blocked.getRequest().getSession().getAttribute("SPRING_SECURITY_CONTEXT"));
        MvcResult otherClient = proxiedLogin("admin", "password", "192.0.2.11");
        assertEquals("/", otherClient.getResponse().getRedirectedUrl());
        assertNotNull(otherClient.getRequest().getSession().getAttribute("SPRING_SECURITY_CONTEXT"));
    }

    @Test
    public void malformedForwardedHeader_cannotBypassFailureRecording() throws Exception {
        for (boolean apiCall : new boolean[] { false, true }) {
            tracker.clear();
            for (int i = 0; i < LIMIT; i++) {
                MvcResult failed = mockMvc.perform(post("/ValidateLogin" + (apiCall ? "?apiCall=true" : ""))
                        .param("loginName", "unknown" + i).param("password", "wrong-password")
                        .header("X-Forwarded-For", ",").session(new MockHttpSession())).andReturn();
                assertEquals(apiCall ? 401 : 302, failed.getResponse().getStatus());
            }
            MvcResult blocked = login("admin", "password", apiCall);
            assertNull(blocked.getRequest().getSession().getAttribute("SPRING_SECURITY_CONTEXT"));
            if (apiCall) {
                assertEquals(401, blocked.getResponse().getStatus());
                assertEquals("{\"error\":\"error.throttled\"}", blocked.getResponse().getContentAsString());
            } else {
                Errors errors = (Errors) blocked.getRequest().getSession().getAttribute(Constants.LOGIN_ERRORS);
                assertNotNull(errors);
                assertEquals("login.error.throttled", errors.getAllErrors().get(0).getCode());
            }
        }
    }

    private MvcResult proxiedLogin(String loginName, String password, String forwarded) throws Exception {
        return mockMvc.perform(post("/ValidateLogin?apiCall=true").param("loginName", loginName)
                .param("password", password).header("X-Forwarded-For", forwarded).with(request -> {
                    request.setRemoteAddr("172.20.1.122");
                    return request;
                }).session(new MockHttpSession())).andReturn();
    }

    private MvcResult login(String loginName, String password, boolean apiCall) throws Exception {
        return mockMvc.perform(post("/ValidateLogin" + (apiCall ? "?apiCall=true" : "")).param("loginName", loginName)
                .param("password", password).session(new MockHttpSession())).andReturn();
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class TestConfig {

        @Bean
        LoginAttemptTracker loginAttemptTracker() throws java.io.IOException {
            java.util.Properties shipped = new java.util.Properties();
            try (var input = java.nio.file.Files
                    .newInputStream(java.nio.file.Path.of("volume/properties/common.properties"))) {
                shipped.load(input);
            }
            return new LoginAttemptTracker(LIMIT, Duration.ofMinutes(15), Clock.systemUTC(),
                    shipped.getProperty("org.openelisglobal.login.throttle.trustedProxies"));
        }

        @Bean
        CustomAuthenticationFailureHandler failureHandler() {
            return new CustomAuthenticationFailureHandler();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("admin").password("{noop}password").roles("ADMIN").build());
        }

        @Bean
        DaoAuthenticationProvider authenticationProvider(UserDetailsService uds) {
            DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
            provider.setUserDetailsService(uds);
            return provider;
        }

        @Bean
        AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
            return config.getAuthenticationManager();
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http, LoginAttemptTracker tracker,
                CustomAuthenticationFailureHandler failureHandler) throws Exception {
            http.addFilterBefore(new LoginThrottleFilter(tracker, failureHandler),
                    UsernamePasswordAuthenticationFilter.class)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .formLogin(form -> form.loginProcessingUrl("/ValidateLogin").usernameParameter("loginName")
                            .passwordParameter("password").failureHandler(failureHandler))
                    .csrf(csrf -> csrf.ignoringRequestMatchers("/ValidateLogin"));
            return http.build();
        }
    }
}
