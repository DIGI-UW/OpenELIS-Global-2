package org.openelisglobal.analyzer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzer.service.AnalyzerDeliveryIssue;
import org.openelisglobal.analyzer.service.AnalyzerDeliveryIssueService;
import org.openelisglobal.analyzer.service.BridgeAnalyzerConnectionException;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@ContextConfiguration(classes = { AnalyzerDeliveryIssuesRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class AnalyzerDeliveryIssuesRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String BASE = "/rest/analyzer/delivery-issues";

    @Autowired
    private AnalyzerDeliveryIssueService deliveryIssueService;

    @Before
    public void resetService() {
        reset(deliveryIssueService);
        when(deliveryIssueService.getOpenIssues()).thenReturn(List
                .of(new AnalyzerDeliveryIssue("ob-1", "DMQ", "12", "GeneXpert bench 1", "conn-7", "10.1.2.3", "ASTM",
                        "DEV0126100001", 5, "2026-09-24T01:00:00Z", "OE_REJECTED", 422, "Unknown test code", true)));
    }

    @Test
    public void listWithoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    public void listWithResultsRole_returns403() throws Exception {
        mockMvc.perform(get(BASE).with(user("results").roles("RESULTS"))).andExpect(status().isForbidden());
        verify(deliveryIssueService, never()).getOpenIssues();
    }

    @Test
    public void listWithAnalyserImportRole_returnsTheJoinedRows() throws Exception {
        mockMvc.perform(get(BASE).with(user("operator").roles("ANALYSER_IMPORT"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.rows[0].analyzerName").value("GeneXpert bench 1"))
                .andExpect(jsonPath("$.data.rows[0].failureReason").value("OE_REJECTED"))
                .andExpect(jsonPath("$.data.rows[0].actionable").value(true));
    }

    @Test
    public void listWhenTheBridgeIsUnreachable_returns502WithAMessageKey() throws Exception {
        when(deliveryIssueService.getOpenIssues())
                .thenThrow(new BridgeAnalyzerConnectionException("analyzer.deliveryIssues.error.bridgeUnreachable"));

        mockMvc.perform(get(BASE).with(user("admin").roles("ADMIN"))).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.messageKey").value("analyzer.deliveryIssues.error.bridgeUnreachable"));
    }

    @Test
    public void retryWithResultsRole_returns403BeforeReachingTheBridge() throws Exception {
        mockMvc.perform(post(BASE + "/ob-1/retry").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
        verify(deliveryIssueService, never()).retry(any(), any());
    }

    @Test
    public void dismissWithResultsRole_returns403BeforeReachingTheBridge() throws Exception {
        mockMvc.perform(post(BASE + "/ob-1/dismiss").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
        verify(deliveryIssueService, never()).dismiss(any(), any());
    }

    @Test
    public void retryWithAnalyserImportRole_retriesThatEntry() throws Exception {
        mockMvc.perform(post(BASE + "/ob-1/retry").with(user("operator").roles("ANALYSER_IMPORT"))
                .requestAttr(IActionConstants.USER_SESSION_DATA, sessionFor(17))).andExpect(status().isOk());
        verify(deliveryIssueService).retry("ob-1", "17");
    }

    @Test
    public void dismissThatTheBridgeRefuses_returns409WithTheReason() throws Exception {
        org.mockito.Mockito
                .doThrow(new BridgeAnalyzerConnectionException("analyzer.deliveryIssues.error.bridgeRefused",
                        java.util.Map.of("status", 409, "reason", "not_dead_lettered")))
                .when(deliveryIssueService).dismiss(any(), any());

        mockMvc.perform(post(BASE + "/ob-1/dismiss").with(user("admin").roles("ADMIN"))
                .requestAttr(IActionConstants.USER_SESSION_DATA, sessionFor(1))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.messageKey").value("analyzer.deliveryIssues.error.bridgeRefused"));
    }

    private static UserSessionData sessionFor(int systemUserId) {
        UserSessionData session = new UserSessionData();
        session.setSytemUserId(systemUserId);
        return session;
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
        AnalyzerDeliveryIssueService deliveryIssueService() {
            return mock(AnalyzerDeliveryIssueService.class);
        }

        @Bean
        AnalyzerDeliveryIssuesRestController analyzerDeliveryIssuesRestController(
                AnalyzerDeliveryIssueService deliveryIssueService) {
            return new AnalyzerDeliveryIssuesRestController(deliveryIssueService);
        }
    }
}
