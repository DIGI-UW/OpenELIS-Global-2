package org.openelisglobal.provider.controller.rest;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.service.ProviderTitleService;
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

/**
 * Provider administration, Reception order entry and the Reports print queue
 * use this controller. Shared search keeps its workflow roles (issue #3966).
 */
@WebAppConfiguration
@ContextConfiguration(classes = { ProviderRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class ProviderRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void getProvider_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/Provider/raw/1")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getProvider_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/Provider/raw/1").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getProvider_receptionReturns403() throws Exception {
        mockMvc.perform(get("/rest/Provider/raw/1").with(user("reception").roles("RECEPTION")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getProvider_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/Provider/raw/1").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void getPerson_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/Provider/Person/1")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getPerson_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/Provider/Person/1").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getPerson_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/Provider/Person/1").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void saveProvider_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post("/rest/Provider/FhirUuid").contentType(MediaType.APPLICATION_JSON)
                .content("{\"externalId\":\"provider-1\",\"active\":true}")).andExpect(status().isUnauthorized());
    }

    @Test
    public void saveProvider_nonAdminReturns403() throws Exception {
        mockMvc.perform(post("/rest/Provider/FhirUuid").contentType(MediaType.APPLICATION_JSON)
                .content("{\"externalId\":\"provider-1\",\"active\":true}").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void saveProvider_adminPassesAuthorization() throws Exception {
        mockMvc.perform(post("/rest/Provider/FhirUuid").contentType(MediaType.APPLICATION_JSON)
                .content("{\"externalId\":\"provider-1\",\"active\":true}").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void searchProviders_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/provider/search").param("search", "lab")).andExpect(status().isUnauthorized());
    }

    @Test
    public void searchProviders_resultsReturns403() throws Exception {
        mockMvc.perform(get("/rest/provider/search").param("search", "lab").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void searchProviders_receptionPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/provider/search").param("search", "lab").with(user("reception").roles("RECEPTION")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void searchProviders_reportsPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/provider/search").param("search", "lab").with(user("reports").roles("REPORTS")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void searchProviders_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/provider/search").param("search", "lab").with(user("admin").roles("ADMIN")))
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
        ProviderService providerService() {
            return mock(ProviderService.class);
        }

        @Bean
        PersonService personService() {
            return mock(PersonService.class);
        }

        @Bean
        ProviderTitleService providerTitleService() {
            return mock(ProviderTitleService.class);
        }

        @Bean
        ProviderRestController providerRestController() {
            return new ProviderRestController();
        }
    }
}
