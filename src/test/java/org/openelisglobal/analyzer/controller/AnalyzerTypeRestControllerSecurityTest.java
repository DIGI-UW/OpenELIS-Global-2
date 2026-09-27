package org.openelisglobal.analyzer.controller;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.analyzer.service.AnalyzerMappingCatalogService;
import org.openelisglobal.analyzer.service.AnalyzerTypeCatalogService;
import org.openelisglobal.analyzer.service.AnalyzerTypeMappingService;
import org.openelisglobal.analyzer.service.BridgeProfileManagementService;
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

@WebAppConfiguration
@ContextConfiguration(classes = { AnalyzerTypeRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class AnalyzerTypeRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void analyzerTypesRejectUnauthenticatedAndUnrelatedRoles() throws Exception {
        mockMvc.perform(get("/rest/analyzer-types").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/rest/analyzer-types").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void analyzerTypesAllowEstablishedAnalyzerAndAdministratorRoles() throws Exception {
        mockMvc.perform(get("/rest/analyzer-types").with(user("analyzer").roles("ANALYSER_IMPORT"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
        mockMvc.perform(
                get("/rest/analyzer-types").with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
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
        AnalyzerTypeCatalogService analyzerTypeCatalogService() {
            return mock(AnalyzerTypeCatalogService.class);
        }

        @Bean
        BridgeProfileManagementService bridgeProfileManagementService() {
            return mock(BridgeProfileManagementService.class);
        }

        @Bean
        AnalyzerMappingCatalogService analyzerMappingCatalogService() {
            return mock(AnalyzerMappingCatalogService.class);
        }

        @Bean
        AnalyzerTypeMappingService analyzerTypeMappingService() {
            return mock(AnalyzerTypeMappingService.class);
        }

        @Bean
        AnalyzerTypeRestController analyzerTypeRestController(AnalyzerTypeCatalogService catalogService,
                BridgeProfileManagementService managementService, AnalyzerMappingCatalogService mappingCatalogService,
                AnalyzerTypeMappingService mappingService) {
            return new AnalyzerTypeRestController(catalogService, managementService, mappingCatalogService,
                    mappingService);
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
