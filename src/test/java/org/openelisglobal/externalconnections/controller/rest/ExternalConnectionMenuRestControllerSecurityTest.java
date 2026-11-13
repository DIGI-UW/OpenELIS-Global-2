package org.openelisglobal.externalconnections.controller.rest;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.externalconnections.service.ExternalConnectionService;
import org.openelisglobal.login.dao.UserModuleService;
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

/**
 * The External Connections administration and search pages use this controller.
 * All are in the Global Administrator-only admin area (issue #3966).
 */
@WebAppConfiguration
@ContextConfiguration(classes = { ExternalConnectionMenuRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class ExternalConnectionMenuRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void showExternalConnectionMenu_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/ExternalConnectionMenu")).andExpect(status().isUnauthorized());
    }

    @Test
    public void showExternalConnectionMenu_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/ExternalConnectionMenu").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void showExternalConnectionMenu_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/ExternalConnectionMenu").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void showExternalConnectionMenu_receptionReturns403() throws Exception {
        mockMvc.perform(get("/rest/ExternalConnectionMenu").with(user("reception").roles("RECEPTION")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void searchExternalConnectionMenu_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/SearchExternalConnectionMenu").param("search", "Y").param("searchString", "lab"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void searchExternalConnectionMenu_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/SearchExternalConnectionMenu").param("search", "Y").param("searchString", "lab")
                .with(user("results").roles("RESULTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void searchExternalConnectionMenu_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/SearchExternalConnectionMenu").param("search", "Y").param("searchString", "lab")
                .with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void deactivateExternalConnection_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post("/rest/DeactivateExternalConnection").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("ID", "1")).andExpect(status().isUnauthorized());
    }

    @Test
    public void deactivateExternalConnection_nonAdminReturns403() throws Exception {
        mockMvc.perform(post("/rest/DeactivateExternalConnection").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("ID", "1")).andExpect(status().isForbidden());
    }

    @Test
    public void deactivateExternalConnection_adminPassesAuthorization() throws Exception {
        mockMvc.perform(post("/rest/DeactivateExternalConnection").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("ID", "1"))
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
        ExternalConnectionService externalConnectionService() {
            ExternalConnectionService service = mock(ExternalConnectionService.class);
            // Use the handled failure path to avoid the global pagination configuration.
            when(service.getAll()).thenThrow(new LIMSRuntimeException("Menu unavailable in security slice"));
            return service;
        }

        @Bean
        UserModuleService userModuleService() {
            return mock(UserModuleService.class);
        }

        @Bean
        PageBuilderService pageBuilderService() {
            return mock(PageBuilderService.class);
        }

        @Bean
        ExternalConnectionMenuRestController externalConnectionMenuRestController() {
            return new ExternalConnectionMenuRestController();
        }
    }
}
