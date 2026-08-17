package org.openelisglobal.qa.controller.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.config.ControllerSetup;
import org.openelisglobal.qa.dto.QaOverviewSummary;
import org.openelisglobal.qa.service.QaOverviewService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Verifies the /rest/qa/overview gate is the qa.view.overview permission
 * authority (QA v1 permission model), not the legacy role list: a role name
 * alone no longer grants access, the derived authority does, and the
 * GLOBAL_ADMIN fallback still works for isAdmin-flag users.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { QaOverviewRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class QaOverviewRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void summary_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/qa/overview/summary")).andExpect(status().isUnauthorized());
    }

    @Test
    public void summary_roleWithoutPermissionAuthorityReturns403() throws Exception {
        // Pre-registry the RECEPTION role name alone was enough; now the
        // qa.view.overview authority (derived from the grant matrix at login)
        // is what admits the request.
        mockMvc.perform(get("/rest/qa/overview/summary").with(user("reception").roles("RECEPTION")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void summary_qaViewOverviewAuthorityReturns200() throws Exception {
        mockMvc.perform(get("/rest/qa/overview/summary")
                .with(user("qaofficer").authorities(new SimpleGrantedAuthority("qa.view.overview"))))
                .andExpect(status().isOk());
    }

    @Test
    public void summary_globalAdminRoleFallbackReturns200() throws Exception {
        mockMvc.perform(get("/rest/qa/overview/summary").with(user("admin").roles("GLOBAL_ADMIN")))
                .andExpect(status().isOk());
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
        QaOverviewService qaOverviewService() {
            // Hand-rolled stub (project convention: no Mockito) — the slice
            // exercises the @PreAuthorize gate, not the aggregation.
            return () -> new QaOverviewSummary();
        }

        @Bean
        QaOverviewRestController qaOverviewRestController(QaOverviewService qaOverviewService) {
            QaOverviewRestController controller = new QaOverviewRestController();
            ReflectionTestUtils.setField(controller, "qaOverviewService", qaOverviewService);
            return controller;
        }

        @Bean
        ControllerSetup controllerSetup() {
            // The real @ControllerAdvice must sit in the slice: live traffic
            // routes @PreAuthorize denials through it, and its generic
            // RuntimeException handler used to turn them into 500s. With it
            // registered, the 403 assertions guard the advice ordering too.
            return new ControllerSetup();
        }
    }
}
