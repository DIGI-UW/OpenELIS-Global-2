package org.openelisglobal.testcatalog.controller;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.service.RangeCoverageValidationService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.openelisglobal.testsamplehandling.service.TestSampleHandlingService;
import org.openelisglobal.testterminology.service.TestTerminologyMappingService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
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
 * FR-004: the unified Test Catalog editor surface is gated on ROLE_ADMIN — the
 * API returns 403 for non-admins and 401 for the unauthenticated.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { TestCatalogEditorRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class TestCatalogEditorRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void getEnvelope_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/1")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getEnvelope_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/1").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getEnvelope_adminUnknownTestReturns404() throws Exception {
        // Admin passes the gate; the (mocked) service returns null → 404, proving
        // the request reached the controller rather than being blocked by auth.
        mockMvc.perform(get("/rest/test-catalog/tests/999999").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    public void saveBasicInfo_nonAdminReturns403() throws Exception {
        mockMvc.perform(put("/rest/test-catalog/tests/1/basic-info").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void saveBasicInfo_adminUnknownTestReturns404() throws Exception {
        // Admin passes the gate; the mocked service returns null for an unknown test
        // → 404, proving the write-path reached the controller past auth.
        mockMvc.perform(put("/rest/test-catalog/tests/999999/basic-info").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());
    }

    @Test
    public void getAnalyzers_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/1/analyzers").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getAnalyzers_adminUnknownTestReturns404() throws Exception {
        // Admin passes the gate; the mocked TestService returns null → 404, proving
        // the read-path reached the controller past auth.
        mockMvc.perform(get("/rest/test-catalog/tests/999999/analyzers").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    public void listSampleTypes_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/sample-types").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getTestOrder_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/sample-types/1/test-order").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void saveTestOrder_nonAdminReturns403() throws Exception {
        mockMvc.perform(put("/rest/test-catalog/sample-types/1/test-order").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void getTerminology_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/1/terminology").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void saveTerminology_nonAdminReturns403() throws Exception {
        mockMvc.perform(put("/rest/test-catalog/tests/1/terminology").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
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
        TestCatalogEditorRestController testCatalogEditorRestController(TestService testService) {
            // Only the auth ordering is under test; the section services are unused here.
            return new TestCatalogEditorRestController(testService, mock(TestResultComponentService.class),
                    mock(TestResultInterpretationService.class), mock(TestResultService.class),
                    mock(ResultLimitService.class), mock(RangeCoverageValidationService.class),
                    mock(TestSampleHandlingService.class), mock(AnalyzerService.class),
                    mock(AnalyzerTestMappingService.class), mock(TypeOfSampleService.class),
                    mock(TypeOfSampleTestService.class), mock(TestTerminologyMappingService.class));
        }
    }
}
