package org.openelisglobal.localization.controller.rest;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.localization.service.SupportedLocaleService;
import org.openelisglobal.localization.valueholder.SupportedLocale;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.valueholder.UserSessionData;
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
 * Language Management and the login layout use this controller. Only locale
 * mutations require Global Administrator access (issue #3966).
 */
@WebAppConfiguration
@ContextConfiguration(classes = { SupportedLocaleRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class SupportedLocaleRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void createLocale_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post("/rest/supportedlocales").contentType(MediaType.APPLICATION_JSON)
                .content("{\"localeCode\":\"en\",\"displayName\":\"English\",\"active\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createLocale_nonAdminReturns403() throws Exception {
        mockMvc.perform(post("/rest/supportedlocales").contentType(MediaType.APPLICATION_JSON)
                .content("{\"localeCode\":\"en\",\"displayName\":\"English\",\"active\":true}")
                .with(user("results").roles("RESULTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void createLocale_receptionReturns403() throws Exception {
        mockMvc.perform(post("/rest/supportedlocales").contentType(MediaType.APPLICATION_JSON)
                .content("{\"localeCode\":\"en\",\"displayName\":\"English\",\"active\":true}")
                .with(user("reception").roles("RECEPTION"))).andExpect(status().isForbidden());
    }

    @Test
    public void createLocale_adminPassesAuthorization() throws Exception {
        mockMvc.perform(post("/rest/supportedlocales").contentType(MediaType.APPLICATION_JSON)
                .content("{\"localeCode\":\"en\",\"displayName\":\"English\",\"active\":true}")
                .with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void updateLocale_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(put("/rest/supportedlocales/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"localeCode\":\"en\",\"displayName\":\"English\",\"active\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void updateLocale_nonAdminReturns403() throws Exception {
        mockMvc.perform(put("/rest/supportedlocales/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"localeCode\":\"en\",\"displayName\":\"English\",\"active\":true}")
                .with(user("results").roles("RESULTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void updateLocale_adminPassesAuthorization() throws Exception {
        mockMvc.perform(put("/rest/supportedlocales/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"localeCode\":\"en\",\"displayName\":\"English\",\"active\":true}")
                .with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void deleteLocale_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(delete("/rest/supportedlocales/1")).andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteLocale_nonAdminReturns403() throws Exception {
        mockMvc.perform(delete("/rest/supportedlocales/1").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void deleteLocale_adminPassesAuthorization() throws Exception {
        mockMvc.perform(delete("/rest/supportedlocales/1").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void setFallback_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post("/rest/supportedlocales/1/setFallback").sessionAttr(IActionConstants.USER_SESSION_DATA,
                sessionData())).andExpect(status().isUnauthorized());
    }

    @Test
    public void setFallback_nonAdminReturns403() throws Exception {
        mockMvc.perform(post("/rest/supportedlocales/1/setFallback")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData()).with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void setFallback_adminPassesAuthorization() throws Exception {
        mockMvc.perform(post("/rest/supportedlocales/1/setFallback")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData()).with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void getAllLocales_resultsPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/supportedlocales").with(user("results").roles("RESULTS")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void getActiveLocales_resultsPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/supportedlocales/active").with(user("results").roles("RESULTS")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    private static UserSessionData sessionData() {
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(1);
        return sessionData;
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
        SupportedLocaleService supportedLocaleService() {
            SupportedLocaleService service = mock(SupportedLocaleService.class);
            // Use handled duplicate/missing-locale paths without persistence or global
            // context.
            when(service.getByLocaleCode("en")).thenReturn(Optional.of(new SupportedLocale()));
            when(service.setFallback("1", "1")).thenThrow(new IllegalArgumentException("Locale not found"));
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
        SupportedLocaleRestController supportedLocaleRestController() {
            return new SupportedLocaleRestController();
        }
    }
}
