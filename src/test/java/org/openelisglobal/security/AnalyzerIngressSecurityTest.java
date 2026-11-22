package org.openelisglobal.security;

import static org.junit.Assert.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

@WebAppConfiguration
@ContextConfiguration(classes = AnalyzerIngressSecurityTest.TestConfig.class)
@TestPropertySource("classpath:common.properties")
public class AnalyzerIngressSecurityTest extends SecuritySliceMockMvcTest {

    private static final String AST_EVENTS = "/rest/analyzer/events/ast";
    private static final String CULTURE_EVENTS = "/rest/analyzer/events/culture";

    @Test
    public void missingCredentialsFailClosed() throws Exception {
        mockMvc.perform(post(AST_EVENTS)).andExpect(status().isUnauthorized());
    }

    @Test
    public void invalidBasicCredentialsFailClosed() throws Exception {
        mockMvc.perform(post(AST_EVENTS).with(httpBasic("bridge", "wrong"))).andExpect(status().isUnauthorized());
    }

    @Test
    public void basicAuthenticatedAccountWithoutAnalyzerImportRoleIsDenied() throws Exception {
        mockMvc.perform(post(AST_EVENTS).with(httpBasic("operator", "operator-pass")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void approvedBridgeIdentityCanSubmitAstAndCultureEvents() throws Exception {
        var astResult = mockMvc.perform(post(AST_EVENTS).with(httpBasic("bridge", "bridge-pass")))
                .andExpect(status().isNoContent()).andReturn();
        mockMvc.perform(post(CULTURE_EVENTS).with(httpBasic("bridge", "bridge-pass")))
                .andExpect(status().isNoContent());
        assertNull("Analyzer ingress must remain stateless", astResult.getRequest().getSession(false));
    }

    @Test
    public void browserSessionIsNotAcceptedAsAnalyzerIngress() throws Exception {
        mockMvc.perform(post(AST_EVENTS).session(browserSessionWithAnalyzerRole()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void analyzerReconciliationRouteStillUsesTheNormalBrowserChain() throws Exception {
        mockMvc.perform(get("/rest/analyzer/import-issues").session(browserSessionWithAnalyzerRole()))
                .andExpect(status().isNoContent());
    }

    private MockHttpSession browserSessionWithAnalyzerRole() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_ANALYSER_IMPORT"))));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class TestConfig {

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("bridge").password("{noop}bridge-pass").roles("ANALYSER_IMPORT").build(),
                    User.withUsername("operator").password("{noop}operator-pass").roles("RESULTS").build());
        }

        @Bean
        @Order(1)
        SecurityFilterChain analyzerIngressSecurityFilterChain(HttpSecurity http) throws Exception {
            SecurityConfig.configureAnalyzerIngress(http);
            return http.build();
        }

        @Bean
        @Order(Ordered.LOWEST_PRECEDENCE)
        SecurityFilterChain browserSecurityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).httpBasic(Customizer.withDefaults())
                    .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean
        SimpleUrlHandlerMapping ingressProbeHandlerMapping() {
            HttpRequestHandler noContent = (request, response) -> response.setStatus(204);
            SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
            mapping.setUrlMap(Map.of(AST_EVENTS, noContent, CULTURE_EVENTS, noContent, "/rest/analyzer/import-issues",
                    noContent));
            mapping.setOrder(-1);
            return mapping;
        }
    }
}
