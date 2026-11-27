package org.openelisglobal.security;

import static org.junit.Assert.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.x509;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.openelisglobal.analyzer.service.AnalyzerBridgePairingService;
import org.openelisglobal.analyzer.service.BridgeTls;
import org.springframework.beans.factory.ObjectProvider;
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
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

@WebAppConfiguration
@ContextConfiguration(classes = AnalyzerBridgeIngressSecurityTest.TestConfig.class)
public class AnalyzerBridgeIngressSecurityTest extends SecuritySliceMockMvcTest {

    private static final String DELIVERY = "/analyzer/fhir";
    private static final String ADMIN_CREDENTIAL = java.util.UUID.randomUUID().toString();
    private static final X509Certificate PAIRED = certificate();
    private static final X509Certificate STRANGER = certificate();

    @Test
    public void thePairedBridgeCertificateDelivers() throws Exception {
        var result = mockMvc.perform(post(DELIVERY).with(x509(PAIRED))).andExpect(status().isNoContent()).andReturn();
        assertNull("Bridge delivery must remain stateless", result.getRequest().getSession(false));
    }

    @Test
    public void anotherCertificateIsRefused() throws Exception {
        mockMvc.perform(post(DELIVERY).with(x509(STRANGER))).andExpect(status().isUnauthorized());
    }

    @Test
    public void noCertificateIsRefused() throws Exception {
        mockMvc.perform(post(DELIVERY)).andExpect(status().isUnauthorized());
    }

    @Test
    public void anAdministratorPasswordIsNotABridgeIdentity() throws Exception {
        mockMvc.perform(post(DELIVERY).with(httpBasic("admin", ADMIN_CREDENTIAL))).andExpect(status().isUnauthorized());
    }

    @Test
    public void anAdministratorBrowserSessionIsNotABridgeIdentity() throws Exception {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_GLOBAL_ADMIN"))));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        mockMvc.perform(post(DELIVERY).session(session)).andExpect(status().isUnauthorized());
    }

    private static X509Certificate certificate() {
        try {
            return BridgeTls.generateIdentity().certificate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class TestConfig {

        @Bean
        AnalyzerBridgePairingService pairing() {
            return new AnalyzerBridgePairingService() {
                @Override
                public Status getStatus() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Status pair(String code, String sysUserId) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Optional<PairedBridge> getPairedBridge() {
                    return Optional.empty();
                }

                @Override
                public boolean isPairedBridge(X509Certificate certificate) {
                    return PAIRED.equals(certificate);
                }

                @Override
                public void pairWithConfiguredCode() {
                }
            };
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(User.withUsername("admin").password("{noop}" + ADMIN_CREDENTIAL)
                    .roles("ADMIN", "GLOBAL_ADMIN", "ANALYSER_IMPORT").build());
        }

        @Bean
        @Order(0)
        SecurityFilterChain analyzerBridgeSecurityFilterChain(HttpSecurity http,
                ObjectProvider<AnalyzerBridgePairingService> pairing) throws Exception {
            SecurityConfig.configureBridgeIngress(http, pairing);
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
        SimpleUrlHandlerMapping deliveryProbeHandlerMapping() {
            HttpRequestHandler noContent = (request, response) -> response.setStatus(204);
            SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
            mapping.setUrlMap(Map.of(DELIVERY, noContent));
            mapping.setOrder(-1);
            return mapping;
        }
    }
}
