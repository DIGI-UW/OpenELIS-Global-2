package org.openelisglobal.analyzer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzerresults.service.AnalyzerFailedRunService;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.view.PageBuilderService;
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
@ContextConfiguration(classes = { AnalyzerFailedRunRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class AnalyzerFailedRunRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String FAILED_RUN = "/rest/analyzer/results/1004/failed-run";

    @Autowired
    private AnalyzerFailedRunService failedRunService;

    @Before
    public void clearInteractions() {
        org.mockito.Mockito.clearInvocations(failedRunService);
    }

    @Test
    public void dismissingAFailedRunRefusesCallersWhoCannotReviewAnalyzerResultsBeforeTouchingTheOrder()
            throws Exception {
        mockMvc.perform(post(FAILED_RUN)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(FAILED_RUN).with(user("results").roles("RESULTS"))).andExpect(status().isForbidden());

        verify(failedRunService, never()).dismissAsFailedRun(any(), any());
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
        AnalyzerFailedRunService analyzerFailedRunService() {
            return mock(AnalyzerFailedRunService.class);
        }

        @Bean
        AnalyzerFailedRunRestController analyzerFailedRunRestController(AnalyzerFailedRunService failedRunService) {
            return new AnalyzerFailedRunRestController(failedRunService);
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
