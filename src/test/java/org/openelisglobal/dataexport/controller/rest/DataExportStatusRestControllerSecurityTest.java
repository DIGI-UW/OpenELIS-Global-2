package org.openelisglobal.dataexport.controller.rest;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.dataexport.service.DataExportStatusViewService;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = { DataExportStatusRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class DataExportStatusRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void getDataExportStatus_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/DataExportStatus").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getDataExportStatus_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/DataExportStatus").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void getDataExportStatus_adminReturns200WithJsonArray() throws Exception {
        mockMvc.perform(get("/rest/DataExportStatus").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test
    public void getAttempts_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/DataExportStatus/1/attempts").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getAttempts_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/DataExportStatus/1/attempts").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void getAttempts_adminReturns200WithJsonArray() throws Exception {
        mockMvc.perform(get("/rest/DataExportStatus/1/attempts").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test
    public void triggerExport_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post("/rest/DataExportStatus/1/trigger").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void triggerExport_nonAdminReturns403() throws Exception {
        mockMvc.perform(post("/rest/DataExportStatus/1/trigger").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void triggerExport_adminReturns200WhenTaskExists() throws Exception {
        mockMvc.perform(post("/rest/DataExportStatus/1/trigger").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    public void triggerExport_adminReturns404WhenTaskMissing() throws Exception {
        mockMvc.perform(post("/rest/DataExportStatus/999/trigger").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
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
        DataExportStatusViewService dataExportStatusViewService() {
            DataExportStatusViewService service = mock(DataExportStatusViewService.class);
            org.mockito.Mockito.when(service.getAllStatuses()).thenReturn(List.of());
            org.mockito.Mockito.when(service.getAttemptsForTask(org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
            org.mockito.Mockito.when(service.triggerExport(1L)).thenReturn(true);
            org.mockito.Mockito.when(service.triggerExport(999L)).thenReturn(false);
            return service;
        }

        @Bean
        DataExportStatusRestController dataExportStatusRestController(
                DataExportStatusViewService dataExportStatusViewService) {
            DataExportStatusRestController controller = new DataExportStatusRestController();
            ReflectionTestUtils.setField(controller, "dataExportStatusViewService", dataExportStatusViewService);
            return controller;
        }
    }
}
