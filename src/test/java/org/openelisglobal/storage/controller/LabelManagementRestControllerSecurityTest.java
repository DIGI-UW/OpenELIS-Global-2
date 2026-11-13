package org.openelisglobal.storage.controller;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.dao.StorageRackDAO;
import org.openelisglobal.storage.dao.StorageShelfDAO;
import org.openelisglobal.storage.service.LabelManagementService;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Storage location labels belong to the Storage pages, so this controller uses
 * the same roles as the other storage controllers (issue #3966).
 */
@WebAppConfiguration
@ContextConfiguration(classes = { LabelManagementRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class LabelManagementRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    // An unknown location type is rejected before any DAO or service call.
    private static final String PRINT_LABEL = "/rest/storage/unknown/1/print-label";
    private static final String PRINT_HISTORY = "/rest/storage/unknown/1/print-history";

    @Test
    public void printLabel_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post(PRINT_LABEL)).andExpect(status().isUnauthorized());
    }

    @Test
    public void printLabel_reportsReturns403() throws Exception {
        mockMvc.perform(post(PRINT_LABEL).with(user("reports").roles("REPORTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void printLabel_receptionPassesAuthorization() throws Exception {
        assertPassesAuthorization(post(PRINT_LABEL), "RECEPTION");
    }

    @Test
    public void printLabel_resultsPassesAuthorization() throws Exception {
        assertPassesAuthorization(post(PRINT_LABEL), "RESULTS");
    }

    @Test
    public void printLabel_adminPassesAuthorization() throws Exception {
        assertPassesAuthorization(post(PRINT_LABEL), "ADMIN");
    }

    @Test
    public void getPrintHistory_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get(PRINT_HISTORY)).andExpect(status().isUnauthorized());
    }

    @Test
    public void getPrintHistory_reportsReturns403() throws Exception {
        mockMvc.perform(get(PRINT_HISTORY).with(user("reports").roles("REPORTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void getPrintHistory_receptionPassesAuthorization() throws Exception {
        assertPassesAuthorization(get(PRINT_HISTORY), "RECEPTION");
    }

    @Test
    public void getPrintHistory_resultsPassesAuthorization() throws Exception {
        assertPassesAuthorization(get(PRINT_HISTORY), "RESULTS");
    }

    @Test
    public void getPrintHistory_adminPassesAuthorization() throws Exception {
        assertPassesAuthorization(get(PRINT_HISTORY), "ADMIN");
    }

    private void assertPassesAuthorization(MockHttpServletRequestBuilder request, String role) throws Exception {
        mockMvc.perform(request.with(user(role.toLowerCase()).roles(role)))
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
        LabelManagementService labelManagementService() {
            return mock(LabelManagementService.class);
        }

        @Bean
        StorageDeviceDAO storageDeviceDAO() {
            return mock(StorageDeviceDAO.class);
        }

        @Bean
        StorageShelfDAO storageShelfDAO() {
            return mock(StorageShelfDAO.class);
        }

        @Bean
        StorageRackDAO storageRackDAO() {
            return mock(StorageRackDAO.class);
        }

        @Bean
        LabelManagementRestController labelManagementRestController() {
            return new LabelManagementRestController();
        }
    }
}
