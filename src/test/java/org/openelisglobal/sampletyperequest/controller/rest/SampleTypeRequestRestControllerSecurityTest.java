package org.openelisglobal.sampletyperequest.controller.rest;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.sampletyperequest.service.SampleTypeRequestService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testmethod.service.TestMethodService;
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

/**
 * Reception order entry and Collect Sample use the sample-type request API.
 * Request access requires Reception or Global Administrator (issue #3966).
 */
@WebAppConfiguration
@ContextConfiguration(classes = { SampleTypeRequestRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class SampleTypeRequestRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void getRequestsBySample_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/sample-type-requests/sample/1")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getRequestsBySample_resultsReturns403() throws Exception {
        mockMvc.perform(get("/rest/sample-type-requests/sample/1").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getRequestsBySample_reportsReturns403() throws Exception {
        mockMvc.perform(get("/rest/sample-type-requests/sample/1").with(user("reports").roles("REPORTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getRequestsBySample_receptionPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/sample-type-requests/sample/1").with(user("reception").roles("RECEPTION")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void getRequestsBySample_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/sample-type-requests/sample/1").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void getPendingRequests_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/sample-type-requests/sample/1/pending")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getPendingRequests_resultsReturns403() throws Exception {
        mockMvc.perform(get("/rest/sample-type-requests/sample/1/pending").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getPendingRequests_reportsReturns403() throws Exception {
        mockMvc.perform(get("/rest/sample-type-requests/sample/1/pending").with(user("reports").roles("REPORTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getPendingRequests_receptionPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/sample-type-requests/sample/1/pending").with(user("reception").roles("RECEPTION")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void getPendingRequests_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/sample-type-requests/sample/1/pending").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void fulfillRequest_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(put("/rest/sample-type-requests/1/fulfill").param("sampleItemId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void fulfillRequest_resultsReturns403() throws Exception {
        mockMvc.perform(put("/rest/sample-type-requests/1/fulfill").param("sampleItemId", "1")
                .with(user("results").roles("RESULTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void fulfillRequest_reportsReturns403() throws Exception {
        mockMvc.perform(put("/rest/sample-type-requests/1/fulfill").param("sampleItemId", "1")
                .with(user("reports").roles("REPORTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void fulfillRequest_receptionPassesAuthorization() throws Exception {
        mockMvc.perform(put("/rest/sample-type-requests/1/fulfill").param("sampleItemId", "1")
                .with(user("reception").roles("RECEPTION")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void fulfillRequest_adminPassesAuthorization() throws Exception {
        mockMvc.perform(put("/rest/sample-type-requests/1/fulfill").param("sampleItemId", "1")
                .with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void cancelRequest_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(put("/rest/sample-type-requests/1/cancel")).andExpect(status().isUnauthorized());
    }

    @Test
    public void cancelRequest_resultsReturns403() throws Exception {
        mockMvc.perform(put("/rest/sample-type-requests/1/cancel").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void cancelRequest_reportsReturns403() throws Exception {
        mockMvc.perform(put("/rest/sample-type-requests/1/cancel").with(user("reports").roles("REPORTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void cancelRequest_receptionPassesAuthorization() throws Exception {
        mockMvc.perform(put("/rest/sample-type-requests/1/cancel").with(user("reception").roles("RECEPTION")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void cancelRequest_adminPassesAuthorization() throws Exception {
        mockMvc.perform(put("/rest/sample-type-requests/1/cancel").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
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
        SampleTypeRequestService sampleTypeRequestService() {
            SampleTypeRequestService service = mock(SampleTypeRequestService.class);
            // Exercise handled missing-request responses without constructing an entity
            // graph.
            doThrow(new IllegalArgumentException("Request not found")).when(service).fulfillRequest(1, "1");
            doThrow(new IllegalArgumentException("Request not found")).when(service).cancelRequest(1);
            return service;
        }

        @Bean
        TestService testService() {
            return mock(TestService.class);
        }

        @Bean
        TestMethodService testMethodService() {
            return mock(TestMethodService.class);
        }

        @Bean
        PanelService panelService() {
            return mock(PanelService.class);
        }

        @Bean
        SampleTypeRequestRestController sampleTypeRequestRestController() {
            return new SampleTypeRequestRestController();
        }
    }
}
