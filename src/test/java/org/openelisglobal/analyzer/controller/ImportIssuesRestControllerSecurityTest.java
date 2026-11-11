package org.openelisglobal.analyzer.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.analyzer.service.AnalyzerEventPersistenceService;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = { ImportIssuesRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class ImportIssuesRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void getImportIssues_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/rest/analyzer/import-issues")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getImportIssues_withResultsRole_returns403() throws Exception {
        mockMvc.perform(get("/rest/analyzer/import-issues").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getImportIssues_withAdminRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/analyzer/import-issues").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    public void getImportIssues_withAnalyserImportRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/analyzer/import-issues").with(user("operator").roles("ANALYSER_IMPORT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.eventRows[0].externalEventId").value("unmatched-ast"))
                .andExpect(jsonPath("$.data.eventRows[0].failureReason").value("AST_ANALYZER_RUN_NOT_MATCHED"))
                .andExpect(jsonPath("$.data.rows").doesNotExist());
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
        AnalyzerEventPersistenceService analyzerEventPersistenceService() {
            AnalyzerEventPersistenceService service = mock(AnalyzerEventPersistenceService.class);
            AnalyzerEvent event = new AnalyzerEvent();
            event.setExternalEventId("unmatched-ast");
            event.setEventType("AST_RESULT_AVAILABLE");
            event.setFailureReason("AST_ANALYZER_RUN_NOT_MATCHED");
            when(service.getFailed(100)).thenReturn(List.of(event));
            return service;
        }

        @Bean
        ImportIssuesRestController importIssuesRestController(
                AnalyzerEventPersistenceService analyzerEventPersistenceService) {
            ImportIssuesRestController controller = new ImportIssuesRestController();
            ReflectionTestUtils.setField(controller, "analyzerEventPersistenceService",
                    analyzerEventPersistenceService);
            return controller;
        }
    }
}
