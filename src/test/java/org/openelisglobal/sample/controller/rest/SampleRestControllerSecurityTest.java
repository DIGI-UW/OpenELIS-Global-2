package org.openelisglobal.sample.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import org.junit.Test;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = { SampleRestControllerSecurityTest.TestConfig.class })
public class SampleRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    // --- getSampleByAccessionNumber (GET
    // /rest/sample/all-by-accession/{accessionNumber}) ---

    @Test
    public void getAllByAccession_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/rest/sample/all-by-accession/ACC123")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getAllByAccession_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(get("/rest/sample/all-by-accession/ACC123").with(user("rep").roles("REPORTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getAllByAccession_receptionRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/sample/all-by-accession/ACC123").with(user("rec").roles("RECEPTION")))
                .andExpect(status().isOk());
    }

    @Test
    public void getAllByAccession_resultsRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/sample/all-by-accession/ACC123").with(user("res").roles("RESULTS")))
                .andExpect(status().isOk());
    }

    @Test
    public void getAllByAccession_adminRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/sample/all-by-accession/ACC123").with(user("adm").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    // --- getUnassignedSampleByAccessionNumber (GET
    // /rest/sample/unassigned-by-accession/{accessionNumber}) ---

    @Test
    public void getUnassignedByAccession_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/rest/sample/unassigned-by-accession/ACC123")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getUnassignedByAccession_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(get("/rest/sample/unassigned-by-accession/ACC123").with(user("rep").roles("REPORTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getUnassignedByAccession_receptionRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/sample/unassigned-by-accession/ACC123").with(user("rec").roles("RECEPTION")))
                .andExpect(status().isOk());
    }

    @Test
    public void getUnassignedByAccession_resultsRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/sample/unassigned-by-accession/ACC123").with(user("res").roles("RESULTS")))
                .andExpect(status().isOk());
    }

    @Test
    public void getUnassignedByAccession_adminRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/sample/unassigned-by-accession/ACC123").with(user("adm").roles("ADMIN")))
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
        SampleService sampleService() {
            SampleService service = mock(SampleService.class);
            Sample sample = new Sample();
            sample.setId("1");
            sample.setAccessionNumber("ACC123");
            when(service.getSampleByAccessionNumber(anyString())).thenReturn(sample);
            when(service.getUnassignedSampleByAccessionNumber(anyString())).thenReturn(sample);
            return service;
        }

        @Bean
        AnalysisService analysisService() {
            AnalysisService service = mock(AnalysisService.class);
            when(service.getAnalysesBySampleIdAndStatusId(any(), any())).thenReturn(Collections.emptyList());
            return service;
        }

        @Bean
        IStatusService statusService() {
            IStatusService service = mock(IStatusService.class);
            when(service.getStatusID(any(StatusService.AnalysisStatus.class))).thenReturn("1");
            return service;
        }

        @Bean
        SampleRestController sampleRestController(SampleService sampleService, AnalysisService analysisService,
                IStatusService statusService) {
            SampleRestController controller = new SampleRestController();
            ReflectionTestUtils.setField(controller, "sampleService", sampleService);
            ReflectionTestUtils.setField(controller, "analysisService", analysisService);
            ReflectionTestUtils.setField(controller, "statusService", statusService);
            return controller;
        }
    }
}
