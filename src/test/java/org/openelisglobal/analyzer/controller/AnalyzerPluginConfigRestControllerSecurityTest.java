package org.openelisglobal.analyzer.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.analyzer.service.AnalyzerPendingCodeService;
import org.openelisglobal.analyzer.service.AnalyzerPluginConfigService;
import org.openelisglobal.analyzer.service.AnalyzerResultValueOptionService;
import org.openelisglobal.analyzer.service.AnalyzerSetupVerificationService;
import org.openelisglobal.analyzer.valueholder.AnalyzerPluginConfig;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = { AnalyzerPluginConfigRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
@ActiveProfiles("analyzer-plugin-config-security-slice")
public class AnalyzerPluginConfigRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void testGetPluginConfig_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/rest/analyzer/analyzers/101/plugin-config").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetPluginConfig_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(get("/rest/analyzer/analyzers/101/plugin-config").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void testGetPluginConfig_GlobalAdminRole_Returns200() throws Exception {
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);

        mockMvc.perform(get("/rest/analyzer/analyzers/101/plugin-config").with(user("admin").roles("GLOBAL_ADMIN"))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, userSessionData)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    public void testUpdatePluginConfig_GenericWriteRouteIsNotAvailable() throws Exception {
        mockMvc.perform(put("/rest/analyzer/analyzers/101/plugin-config").with(user("admin").roles("GLOBAL_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"resultValueMappings\":[]}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void testAnalyzerConfigurationEndpoints_WithoutAuthentication_Return401() throws Exception {
        for (MockHttpServletRequestBuilder request : protectedConfigurationRequests()) {
            mockMvc.perform(request).andExpect(status().isUnauthorized());
        }
    }

    @Test
    public void testAnalyzerConfigurationEndpoints_NonAdminRole_Return403() throws Exception {
        for (MockHttpServletRequestBuilder request : protectedConfigurationRequests()) {
            mockMvc.perform(request.with(user("results").roles("RESULTS"))).andExpect(status().isForbidden());
        }
    }

    private List<MockHttpServletRequestBuilder> protectedConfigurationRequests() {
        return List.of(get("/rest/analyzer/analyzers/101/pending-codes"),
                get("/rest/analyzer/analyzers/101/test-mapping-options"),
                put("/rest/analyzer/analyzers/101/pending-codes/pc-1/status").content("{\"status\":\"IGNORED\"}"),
                post("/rest/analyzer/analyzers/101/pending-codes/pc-1/resolve").content("{\"openelisTestId\":\"501\"}"),
                get("/rest/analyzer/analyzers/101/result-value-mappings"),
                get("/rest/analyzer/analyzers/101/result-value-options").param("testCode", "MTB"),
                put("/rest/analyzer/analyzers/101/result-value-mappings").content("[]"),
                get("/rest/analyzer/analyzers/101/pending-result-values"),
                post("/rest/analyzer/analyzers/101/pending-result-values/rv-1/resolve")
                        .content("{\"openelisResultOptionId\":\"9001\"}"),
                get("/rest/analyzer/analyzers/101/setup-verification"),
                post("/rest/analyzer/analyzers/101/setup-verification").content("{\"mappingIds\":[],\"qcIds\":[]}"))
                .stream().map(request -> request.contentType(MediaType.APPLICATION_JSON)).toList();
    }

    @Configuration
    @Profile("analyzer-plugin-config-security-slice")
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
        AnalyzerPluginConfigService analyzerPluginConfigService() {
            AnalyzerPluginConfigService service = mock(AnalyzerPluginConfigService.class);
            when(service.getOrCreate("101", "1")).thenReturn(new AnalyzerPluginConfig());
            when(service.getConfigAsMap("101")).thenReturn(Map.of("connectionRole", "SERVER"));
            return service;
        }

        @Bean
        AnalyzerPendingCodeService analyzerPendingCodeService() {
            return mock(AnalyzerPendingCodeService.class);
        }

        @Bean
        AnalyzerResultValueOptionService analyzerResultValueOptionService() {
            return mock(AnalyzerResultValueOptionService.class);
        }

        @Bean
        AnalyzerSetupVerificationService analyzerSetupVerificationService() {
            return mock(AnalyzerSetupVerificationService.class);
        }

        @Bean
        AnalyzerPluginConfigRestController analyzerPluginConfigRestController(
                AnalyzerPluginConfigService analyzerPluginConfigService,
                AnalyzerPendingCodeService analyzerPendingCodeService,
                AnalyzerResultValueOptionService analyzerResultValueOptionService,
                AnalyzerSetupVerificationService analyzerSetupVerificationService) {
            AnalyzerPluginConfigRestController controller = new AnalyzerPluginConfigRestController();
            ReflectionTestUtils.setField(controller, "analyzerPluginConfigService", analyzerPluginConfigService);
            ReflectionTestUtils.setField(controller, "analyzerPendingCodeService", analyzerPendingCodeService);
            ReflectionTestUtils.setField(controller, "analyzerResultValueOptionService",
                    analyzerResultValueOptionService);
            ReflectionTestUtils.setField(controller, "analyzerSetupVerificationService",
                    analyzerSetupVerificationService);
            return controller;
        }
    }
}
