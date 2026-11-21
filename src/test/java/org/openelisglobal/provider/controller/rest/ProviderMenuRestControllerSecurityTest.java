package org.openelisglobal.provider.controller.rest;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.provider.service.ProviderService;
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
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The Provider Management administration and search pages use this controller.
 * These pages require Global Administrator access (issue #3966).
 */
@WebAppConfiguration
@ContextConfiguration(classes = { ProviderMenuRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class ProviderMenuRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void showProviderMenu_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/ProviderMenu")).andExpect(status().isUnauthorized());
    }

    @Test
    public void showProviderMenu_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/ProviderMenu").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void showProviderMenu_receptionReturns403() throws Exception {
        mockMvc.perform(get("/rest/ProviderMenu").with(user("reception").roles("RECEPTION")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void showProviderMenu_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/ProviderMenu").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void searchProviderMenu_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/SearchProviderMenu").param("search", "Y").param("searchString", "lab"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void searchProviderMenu_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/SearchProviderMenu").param("search", "Y").param("searchString", "lab")
                .with(user("results").roles("RESULTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void searchProviderMenu_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/SearchProviderMenu").param("search", "Y").param("searchString", "lab")
                .with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void deleteProvider_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post("/rest/DeleteProvider").contentType(MediaType.APPLICATION_FORM_URLENCODED).param("ID", "1")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData())).andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteProvider_nonAdminReturns403() throws Exception {
        mockMvc.perform(post("/rest/DeleteProvider").contentType(MediaType.APPLICATION_FORM_URLENCODED).param("ID", "1")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData()).with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void deleteProvider_adminPassesAuthorization() throws Exception {
        mockMvc.perform(post("/rest/DeleteProvider").contentType(MediaType.APPLICATION_FORM_URLENCODED).param("ID", "1")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData()).with(user("admin").roles("ADMIN")))
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
    static class TestConfig implements WebMvcConfigurer {
        // The slice has no Bean Validation provider, so form binding gets a validator
        // that accepts everything. Only the role check is under test here.
        @Override
        public Validator getValidator() {
            return new Validator() {
                @Override
                public boolean supports(Class<?> clazz) {
                    return true;
                }

                @Override
                public void validate(Object target, Errors errors) {
                }
            };
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).httpBasic(Customizer.withDefaults())
                    .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean
        ProviderService providerService() {
            ProviderService service = mock(ProviderService.class);
            // Use the handled failure path to avoid the global pagination configuration.
            when(service.getPagesOfSearchedProviders(1, "", null))
                    .thenThrow(new LIMSRuntimeException("Menu unavailable in security slice"));
            when(service.getPagesOfSearchedProviders(1, "lab", null))
                    .thenThrow(new LIMSRuntimeException("Menu unavailable in security slice"));
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
        ProviderMenuRestController providerMenuRestController() {
            return new ProviderMenuRestController();
        }
    }
}
