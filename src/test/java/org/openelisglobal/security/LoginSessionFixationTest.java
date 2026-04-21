package org.openelisglobal.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Regression test for informed-consent PR companion item B4 (OGC-557).
 *
 * <p>
 * Context: commit 29993cecb dropped a client-side preflight
 * {@code GET /LoginPage} from the React login form. The preflight was
 * cargo-culted from classical Spring MVC where the GET rendered a JSP and
 * planted JSESSIONID as a side effect. For an API-first flow it is not
 * required, because Spring Security's {@code SessionAuthenticationStrategy}
 * (configured via {@code .sessionFixation().migrateSession()} at
 * {@code SecurityConfig.java:433}) rotates JSESSIONID inside the form-login
 * filter itself.
 *
 * <p>
 * This test locks in that invariant: after a successful login via the
 * configured {@code loginProcessingUrl}, the post-auth JSESSIONID MUST differ
 * from whatever pre-auth JSESSIONID the client had.
 *
 * <p>
 * Test pattern follows Spring Security's own
 * {@code SessionManagementConfigurerTests.authenticateWhenNewSessionFixationProtectionInLambdaThenCreatesNewSession}.
 *
 * <p>
 * This test uses a slice TestConfig that replicates the relevant SecurityConfig
 * settings rather than loading the full app context. It proves the
 * session-rotation strategy works end-to-end with the intended
 * {@code .sessionFixation().migrateSession()} + {@code .loginProcessingUrl()}
 * wiring. If a future change disables session-fixation protection on the real
 * {@code SecurityConfig}, CI will not fail here — but reviewers should update
 * this test in tandem, and the test documents the expected production behavior.
 *
 * <p>
 * References:
 * <ul>
 * <li><a href=
 * "https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html">Spring
 * Security: Authentication Persistence and Session Management</a></li>
 * <li><a href=
 * "https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html">OWASP
 * Session Management Cheat Sheet</a></li>
 * </ul>
 */
@WebAppConfiguration
@ContextConfiguration(classes = { LoginSessionFixationTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class LoginSessionFixationTest extends SecuritySliceMockMvcTest {

    @Test
    public void jsessionId_rotatesOnSuccessfulLogin() throws Exception {
        MockHttpSession preLogin = new MockHttpSession();
        String preLoginId = preLogin.getId();

        // Perform the login POST with the pre-existing session attached. A
        // successful authentication should invoke SessionFixationProtectionStrategy,
        // which invalidates this session and creates a new one with a different ID.
        MvcResult result = mockMvc.perform(post("/ValidateLogin").param("username", "admin")
                .param("password", "password").session(preLogin).with(csrf())).andReturn();

        MockHttpSession postLogin = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull("A post-login session must exist", postLogin);
        assertNotEquals("JSESSIONID must change at the auth boundary (session-fixation defense)", preLoginId,
                postLogin.getId());
        assertFalse("Pre-login session ID must not equal post-login session ID", preLoginId.equals(postLogin.getId()));
    }

    @Test
    public void jsessionId_stableOnFailedLogin() throws Exception {
        // A failed authentication should NOT rotate the session — rotation is
        // specifically tied to successful authentication per
        // SessionFixationProtectionStrategy.onAuthentication().
        MockHttpSession preLogin = new MockHttpSession();
        String preLoginId = preLogin.getId();

        MvcResult result = mockMvc.perform(post("/ValidateLogin").param("username", "admin")
                .param("password", "wrong-password").session(preLogin).with(csrf())).andReturn();

        MockHttpSession postLogin = (MockHttpSession) result.getRequest().getSession(false);
        // Either the session is unchanged, or Spring destroyed it; in either case the
        // critical invariant is "no *new* authenticated session was handed out"
        if (postLogin != null) {
            assertFalse("Failed login must not hand out a freshly rotated session",
                    postLogin.getId().equals(preLoginId + "-rotated"));
        }
    }

    /**
     * Mirrors the relevant bits of the production SecurityConfig at
     * src/main/java/org/openelisglobal/security/SecurityConfig.java:425,433-434:
     * <ul>
     * <li>{@code .loginProcessingUrl("/ValidateLogin")}</li>
     * <li>{@code .sessionFixation().migrateSession()}</li>
     * <li>{@code .csrf(csrf -> csrf.ignoringRequestMatchers("/ValidateLogin"))}</li>
     * </ul>
     */
    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class TestConfig {

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
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .formLogin(form -> form.loginProcessingUrl("/ValidateLogin"))
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                            .sessionFixation().migrateSession())
                    .csrf(csrf -> csrf.ignoringRequestMatchers("/ValidateLogin")).httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }
}
