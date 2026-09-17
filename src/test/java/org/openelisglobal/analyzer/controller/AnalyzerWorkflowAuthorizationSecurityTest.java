package org.openelisglobal.analyzer.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.analyzer.service.AnalyzerActivationService;
import org.openelisglobal.analyzer.service.AnalyzerConnectionProbeService;
import org.openelisglobal.analyzer.service.AnalyzerInstanceService;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.qc.controller.QCRestController;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.service.QCDashboardService;
import org.openelisglobal.qc.service.QCStatisticsService;
import org.openelisglobal.qc.service.WestgardRuleConfigService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.view.PageBuilderService;
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

@WebAppConfiguration
@ContextConfiguration(classes = { AnalyzerWorkflowAuthorizationSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class AnalyzerWorkflowAuthorizationSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void unrelatedAuthenticatedRoleCannotOpenAnalyzerSetup() throws Exception {
        mockMvc.perform(get("/rest/analyzer/analyzers").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void unrelatedAuthenticatedRoleCannotProbeAnalyzerConnection() throws Exception {
        mockMvc.perform(post("/rest/analyzer/analyzers/77/test-connection").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void establishedAnalyzerRoleCanReadActivationReadiness() throws Exception {
        mockMvc.perform(get("/rest/analyzer/analyzers/77/activation-readiness")
                .with(user("analyzer").roles("ANALYSER_IMPORT")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void unrelatedAuthenticatedRoleCannotOpenLinkedOperationalQc() throws Exception {
        mockMvc.perform(get("/rest/qc/control-lots").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void establishedAnalyzerRoleCanOpenSetupProbeAndLinkedQc() throws Exception {
        mockMvc.perform(get("/rest/analyzer/analyzers").with(user("analyzer").roles("ANALYSER_IMPORT"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
        mockMvc.perform(post("/rest/analyzer/analyzers/77/test-connection")
                .with(user("analyzer").roles("ANALYSER_IMPORT")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(get("/rest/qc/control-lots").with(user("analyzer").roles("ANALYSER_IMPORT"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
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
        AnalyzerInstanceService analyzerInstanceService() {
            AnalyzerInstanceService service = mock(AnalyzerInstanceService.class);
            when(service.list()).thenReturn(List.of());
            return service;
        }

        @Bean
        AnalyzerConnectionProbeService analyzerConnectionProbeService() {
            return mock(AnalyzerConnectionProbeService.class);
        }

        @Bean
        AnalyzerActivationService analyzerActivationService() {
            return mock(AnalyzerActivationService.class);
        }

        @Bean
        AnalyzerInstanceRestController analyzerInstanceRestController(AnalyzerInstanceService service) {
            return new AnalyzerInstanceRestController(service);
        }

        @Bean
        AnalyzerConnectionProbeRestController analyzerConnectionProbeRestController(
                AnalyzerConnectionProbeService service) {
            return new AnalyzerConnectionProbeRestController(service);
        }

        @Bean
        AnalyzerActivationRestController analyzerActivationRestController(AnalyzerActivationService service) {
            return new AnalyzerActivationRestController(service);
        }

        @Bean
        QCRestController qcRestController() {
            return new QCRestController();
        }

        @Bean
        QCControlLotService qcControlLotService() {
            QCControlLotService service = mock(QCControlLotService.class);
            when(service.getAllControlLots()).thenReturn(List.of());
            return service;
        }

        @Bean
        QCStatisticsService qcStatisticsService() {
            return mock(QCStatisticsService.class);
        }

        @Bean
        WestgardRuleConfigService westgardRuleConfigService() {
            return mock(WestgardRuleConfigService.class);
        }

        @Bean
        QCDashboardService qcDashboardService() {
            return mock(QCDashboardService.class);
        }

        @Bean
        UserModuleService userModuleService() {
            return mock(UserModuleService.class);
        }

        @Bean
        PageBuilderService pageBuilderService() {
            return mock(PageBuilderService.class);
        }
    }
}
