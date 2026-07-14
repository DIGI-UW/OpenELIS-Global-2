package org.openelisglobal.esig.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.config.ControllerSetup;
import org.openelisglobal.esig.service.ElectronicSignatureService;
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
 * Verifies the /rest/esig/log gate is the qa.view.qms permission authority (QA
 * permission model): a role name alone does not grant access, the derived
 * authority does, and the GLOBAL_ADMIN fallback still works. Also covers the
 * endpoint's parameter validation once past the gate.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { ElectronicSignatureLogSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class ElectronicSignatureLogSecurityTest extends SecuritySliceMockMvcTest {

    private static final String LOG_URL = "/rest/esig/log?fromDate=2026-06-01&toDate=2026-07-01";

    @Test
    public void log_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get(LOG_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    public void log_roleWithoutPermissionAuthorityReturns403() throws Exception {
        mockMvc.perform(get(LOG_URL).with(user("validator").roles("VALIDATION"))).andExpect(status().isForbidden());
    }

    @Test
    public void log_qaViewQmsAuthorityReturns200() throws Exception {
        mockMvc.perform(get(LOG_URL).with(user("qaofficer").authorities(new SimpleGrantedAuthority("qa.view.qms"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    public void log_globalAdminRoleFallbackReturns200() throws Exception {
        mockMvc.perform(get(LOG_URL).with(user("admin").roles("GLOBAL_ADMIN"))).andExpect(status().isOk());
    }

    @Test
    public void log_invalidMeaningReturns400() throws Exception {
        mockMvc.perform(get(LOG_URL + "&meaning=BOGUS")
                .with(user("qaofficer").authorities(new SimpleGrantedAuthority("qa.view.qms"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void log_reversedDateRangeReturns400() throws Exception {
        mockMvc.perform(get("/rest/esig/log?fromDate=2026-07-01&toDate=2026-06-01")
                .with(user("qaofficer").authorities(new SimpleGrantedAuthority("qa.view.qms"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void log_rangeOverOneYearReturns400() throws Exception {
        mockMvc.perform(get("/rest/esig/log?fromDate=2024-01-01&toDate=2026-07-01")
                .with(user("qaofficer").authorities(new SimpleGrantedAuthority("qa.view.qms"))))
                .andExpect(status().isBadRequest());
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
        ElectronicSignatureService electronicSignatureService() {
            // The slice exercises the @PreAuthorize gate and request
            // validation, not the query.
            ElectronicSignatureService service = mock(ElectronicSignatureService.class);
            when(service.searchSignatures(any(), any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(List.of());
            when(service.countSearchSignatures(any(), any(), any(), any(), any())).thenReturn(0L);
            return service;
        }

        @Bean
        ElectronicSignatureRestController electronicSignatureRestController(ElectronicSignatureService service) {
            ElectronicSignatureRestController controller = new ElectronicSignatureRestController();
            ReflectionTestUtils.setField(controller, "electronicSignatureService", service);
            return controller;
        }

        @Bean
        ControllerSetup controllerSetup() {
            // Real @ControllerAdvice in the slice so @PreAuthorize denials
            // surface as 403s, not 500s (guards the advice ordering).
            return new ControllerSetup();
        }
    }
}
