package org.openelisglobal.testcatalog.controller;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testactivation.service.TestActivationAcknowledgmentService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogActivationRestController;
import org.openelisglobal.testcatalog.service.RangeCoverageValidationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * FR-004 / H-03: test activation is the most security-sensitive new endpoint —
 * it flips {@code is_active} and writes an acknowledgment audit row. It lives
 * on its own controller, so it needs its own auth-ordering coverage: ROLE_ADMIN
 * only, 401 for the unauthenticated, 403 for non-admins.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { TestCatalogActivationRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class TestCatalogActivationRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void activate_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(
                post("/rest/test-catalog/tests/1/activate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void activate_nonAdminReturns403() throws Exception {
        mockMvc.perform(post("/rest/test-catalog/tests/1/activate").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void activate_adminUnknownTestReturns404() throws Exception {
        // Admin passes the gate; the mocked service returns null for an unknown test
        // → 404, proving the write-path reached the controller past auth.
        mockMvc.perform(post("/rest/test-catalog/tests/999999/activate").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).httpBasic(Customizer.withDefaults())
                    .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean
        TestService testService() {
            return mock(TestService.class);
        }

        @Bean
        TestCatalogActivationRestController testActivationRestController(TestService testService) {
            // Only the auth ordering is under test; the collaborators are unused here.
            return new TestCatalogActivationRestController(testService, mock(ResultLimitService.class),
                    mock(RangeCoverageValidationService.class), mock(TestActivationAcknowledgmentService.class));
        }
    }
}
