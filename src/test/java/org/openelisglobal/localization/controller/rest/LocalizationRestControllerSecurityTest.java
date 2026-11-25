package org.openelisglobal.localization.controller.rest;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Test;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationValueService;
import org.openelisglobal.localization.service.SupportedLocaleService;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.view.PageBuilderService;
import org.springframework.beans.factory.annotation.Autowired;
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
 * OGC-1457: a localization value is a test's name (and every other localized
 * label), so changing one is a catalog administration write. Every write on
 * /rest/localizations needs the admin role the Test Catalog editor requires; a
 * receptionist or a lab technician is refused and nothing is written.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { LocalizationRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class LocalizationRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private LocalizationValueService localizationValueService;

    @After
    public void resetMocks() {
        reset(localizationService, localizationValueService);
    }

    @Test
    public void updateTranslations_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(put("/rest/localizations/7/translations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"en\":\"Renamed\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    public void updateTranslations_receptionistIsRefusedAndNothingIsWritten() throws Exception {
        mockMvc.perform(put("/rest/localizations/7/translations").with(user("qa_recept").roles("RECEPTION"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"en\":\"Renamed\"}"))
                .andExpect(status().isForbidden());
        verify(localizationValueService, never()).setTranslation(anyString(), anyString(), any(), any());
    }

    @Test
    public void updateTranslations_labTechnicianIsRefused() throws Exception {
        mockMvc.perform(put("/rest/localizations/7/translations").with(user("qa_labtech").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"en\":\"Renamed\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void updateTranslations_adminReachesTheController() throws Exception {
        mockMvc.perform(put("/rest/localizations/7/translations").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"en\":\"Renamed\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void setTranslation_nonAdminIsRefused() throws Exception {
        mockMvc.perform(post("/rest/localizations/7/translations/fr").with(user("qa_labtech").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"Renommé\"}"))
                .andExpect(status().isForbidden());
        verify(localizationValueService, never()).setTranslation(anyString(), anyString(), any(), any());
    }

    @Test
    public void setTranslation_adminReachesTheController() throws Exception {
        mockMvc.perform(post("/rest/localizations/7/translations/fr").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"Renommé\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void importTranslations_nonAdminIsRefused() throws Exception {
        mockMvc.perform(post("/rest/localizations/import").with(user("qa_recept").roles("RECEPTION"))
                .contentType(MediaType.APPLICATION_JSON).content("[{\"id\":\"7\",\"translations\":{\"en\":\"X\"}}]"))
                .andExpect(status().isForbidden());
        verify(localizationValueService, never()).setTranslation(anyString(), anyString(), any(), any());
    }

    @Test
    public void importTranslations_adminReachesTheController() throws Exception {
        mockMvc.perform(post("/rest/localizations/import").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(result -> assertNotEquals("the admin passes the role check", 403,
                        result.getResponse().getStatus()));
    }

    @Test
    public void readingALocalization_staysOpenToSignedInUsers() throws Exception {
        mockMvc.perform(get("/rest/localizations/7").with(user("qa_labtech").roles("RESULTS")))
                .andExpect(status().isNotFound());
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
        LocalizationService localizationService() {
            return mock(LocalizationService.class);
        }

        @Bean
        LocalizationValueService localizationValueService() {
            return mock(LocalizationValueService.class);
        }

        @Bean
        SupportedLocaleService supportedLocaleService() {
            return mock(SupportedLocaleService.class);
        }

        @Bean
        TestService testService() {
            return mock(TestService.class);
        }

        @Bean
        TypeOfSampleService typeOfSampleService() {
            return mock(TypeOfSampleService.class);
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
        LocalizationRestController localizationRestController() {
            return new LocalizationRestController();
        }
    }
}
