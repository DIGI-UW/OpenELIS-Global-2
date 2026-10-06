package org.openelisglobal.analyzer.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzer.service.AnalyzerMappingEditorService;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
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

@WebAppConfiguration
@ContextConfiguration(classes = { AnalyzerMappingRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class AnalyzerMappingRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String MAPPING = "/rest/analyzer/analyzers/42/mapping";
    private static final String EMPTY_UPDATE = "{\"tests\":[],\"results\":[]}";

    @Autowired
    private AnalyzerMappingEditorService mappingService;

    @Before
    public void clearInteractions() {
        org.mockito.Mockito.clearInvocations(mappingService);
    }

    @Test
    public void mappingCommandsRejectUnauthenticatedCallersBeforeTouchingTheMapping() throws Exception {
        mockMvc.perform(get(MAPPING)).andExpect(status().isUnauthorized());
        mockMvc.perform(put(MAPPING).contentType(MediaType.APPLICATION_JSON).content(EMPTY_UPDATE))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(MAPPING + "/confirm").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        verify(mappingService, never()).saveMapping(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void mappingCommandsRejectRolesUnrelatedToAnalyzerSetupBeforeTouchingTheMapping() throws Exception {
        mockMvc.perform(get(MAPPING).with(user("results").roles("RESULTS"))).andExpect(status().isForbidden());
        mockMvc.perform(put(MAPPING).with(user("results").roles("RESULTS")).contentType(MediaType.APPLICATION_JSON)
                .content(EMPTY_UPDATE)).andExpect(status().isForbidden());
        mockMvc.perform(post(MAPPING + "/confirm").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());

        verify(mappingService, never()).saveMapping(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(mappingService, never()).confirmMapping(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void mappingReadsAllowAnalyzerSetupAndAdministratorRoles() throws Exception {
        mockMvc.perform(get(MAPPING).with(user("analyzer").roles("ANALYSER_IMPORT"))).andExpect(status().isOk());
        mockMvc.perform(get(MAPPING).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
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
        AnalyzerMappingEditorService analyzerMappingEditorService() {
            return mock(AnalyzerMappingEditorService.class);
        }

        @Bean
        AnalyzerMappingRestController analyzerMappingRestController(AnalyzerMappingEditorService mappingService) {
            return new AnalyzerMappingRestController(mappingService);
        }

        @Bean
        UserModuleService userModuleService() {
            return mock(UserModuleService.class);
        }

        @Bean
        PageBuilderService pageBuilderService() {
            return mock(PageBuilderService.class);
        }
    }
}
