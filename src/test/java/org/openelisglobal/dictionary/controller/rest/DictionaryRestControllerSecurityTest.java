package org.openelisglobal.dictionary.controller.rest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.validator.DictionaryFormValidator;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionaryterminology.service.DictionaryTerminologyMappingService;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.view.PageBuilderService;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * The dictionary lookups shared with bench screens (an order's cancel reasons)
 * are read by bench roles, while the dictionary administration stays with
 * administrators. The controller's model and binder hooks run before every
 * handler, so they must not carry the administrators-only rule.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { DictionaryRestControllerSecurityTest.TestConfig.class })
public class DictionaryRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String CANCEL_REASONS = "/rest/dictionary/categories/Order cancel reasons/entries";

    @Test
    public void aBenchUserReadsTheCancelReasons() throws Exception {
        mockMvc.perform(get(CANCEL_REASONS).with(user("bench").roles("RECEPTION"))).andExpect(status().isOk());
        mockMvc.perform(get(CANCEL_REASONS).with(user("analyst").roles("RESULTS"))).andExpect(status().isOk());
    }

    @Test
    public void anAdministratorReadsTheCancelReasons() throws Exception {
        mockMvc.perform(get(CANCEL_REASONS).with(user("manager").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    public void aBenchUserStillCannotOpenTheDictionaryAdministration() throws Exception {
        mockMvc.perform(get("/rest/Dictionary").with(user("analyst").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void anAnonymousCallerIsRefused() throws Exception {
        mockMvc.perform(get(CANCEL_REASONS)).andExpect(status().isUnauthorized());
    }

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
        DictionaryService dictionaryService() {
            DictionaryService service = mock(DictionaryService.class);
            when(service.getActiveSortedEntriesByCategoryName(anyString())).thenReturn(List.of());
            return service;
        }

        @Bean
        DictionaryFormValidator dictionaryFormValidator() {
            return mock(DictionaryFormValidator.class);
        }

        @Bean
        DictionaryCategoryService dictionaryCategoryService() {
            return mock(DictionaryCategoryService.class);
        }

        @Bean
        DictionaryTerminologyMappingService dictionaryTerminologyMappingService() {
            return mock(DictionaryTerminologyMappingService.class);
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
        DictionaryRestController dictionaryRestController() {
            return new DictionaryRestController();
        }
    }
}
