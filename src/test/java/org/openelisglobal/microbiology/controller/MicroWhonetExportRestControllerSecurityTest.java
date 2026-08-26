package org.openelisglobal.microbiology.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroWhonetExportRestController;
import org.openelisglobal.microbiology.form.MicroWhonetPreviewForm;
import org.openelisglobal.reports.service.MicroWhonetExportResult;
import org.openelisglobal.reports.service.WHONetReportService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
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
@ContextConfiguration(classes = { MicroWhonetExportRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class MicroWhonetExportRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String PREVIEW_URL = "/rest/microbiology/whonet/preview?from=2026-07-01&to=2026-07-31";

    @Test
    public void previewWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get(PREVIEW_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    public void previewWithUnrelatedRoleReturns403() throws Exception {
        mockMvc.perform(get(PREVIEW_URL).with(user("user").roles("USER"))).andExpect(status().isForbidden());
    }

    @Test
    public void previewWithReportCapableRoleReachesController() throws Exception {
        mockMvc.perform(get(PREVIEW_URL).with(user("reports").roles("REPORTS"))).andExpect(status().isOk());
    }

    @Test
    public void generateWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post("/rest/microbiology/whonet/exports").contentType(MediaType.APPLICATION_JSON)
                .content(exportQuery())).andExpect(status().isUnauthorized());
    }

    @Test
    public void generateWithUnrelatedRoleReturns403() throws Exception {
        mockMvc.perform(post("/rest/microbiology/whonet/exports").with(user("user").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON).content(exportQuery())).andExpect(status().isForbidden());
    }

    @Test
    public void generateWithReportCapableRoleReachesController() throws Exception {
        UserSessionData session = new UserSessionData();
        session.setSytemUserId(42);
        mockMvc.perform(post("/rest/microbiology/whonet/exports").with(user("results").roles("RESULTS"))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, session).contentType(MediaType.APPLICATION_JSON)
                .content(exportQuery())).andExpect(status().isOk());
    }

    private String exportQuery() {
        return "{\"from\":\"2026-07-01\",\"to\":\"2026-07-31\",\"significance\":\"ALL\",\"dedup\":\"NONE\"}";
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
        WHONetReportService whonetReportService() {
            WHONetReportService service = mock(WHONetReportService.class);
            when(service.previewMicrobiologyExport(org.mockito.ArgumentMatchers.any()))
                    .thenReturn(new MicroWhonetPreviewForm());
            when(service.generateMicrobiologyExport(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.eq("42")))
                    .thenReturn(new MicroWhonetExportResult("WHONET.csv", "csv".getBytes(StandardCharsets.UTF_8)));
            return service;
        }

        @Bean
        MicroWhonetExportRestController microWhonetExportRestController(WHONetReportService service) {
            return new MicroWhonetExportRestController(service);
        }
    }
}
