package org.openelisglobal.analyzer.controller;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.Test;
import org.openelisglobal.analyzer.dao.AnalyzerPluginConfigDAO;
import org.openelisglobal.analyzer.service.AnalyzerPendingCodeService;
import org.openelisglobal.analyzer.service.AnalyzerPluginConfigService;
import org.openelisglobal.analyzer.valueholder.AnalyzerPluginConfig;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = { AnalyzerPluginConfigRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class AnalyzerPluginConfigRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void testGetPluginConfig_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/rest/analyzer/analyzers/101/plugin-config").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetPluginConfig_WithoutPrivilege_Returns403() throws Exception {
        mockMvc.perform(get("/rest/analyzer/analyzers/101/plugin-config").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void testGetPluginConfig_WithPrivilege_Returns200() throws Exception {
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);

        mockMvc.perform(get("/rest/analyzer/analyzers/101/plugin-config")
                .with(user("admin").authorities(new SimpleGrantedAuthority("PRIV_ANALYZER_CONFIGURE")))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, userSessionData)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
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
        AnalyzerPluginConfigService analyzerPluginConfigService() {
            return new NoOpAnalyzerPluginConfigService();
        }

        @Bean
        AnalyzerPendingCodeService analyzerPendingCodeService() {
            return mock(AnalyzerPendingCodeService.class);
        }

        @Bean
        AnalyzerPluginConfigRestController analyzerPluginConfigRestController(
                AnalyzerPluginConfigService analyzerPluginConfigService,
                AnalyzerPendingCodeService analyzerPendingCodeService) {
            AnalyzerPluginConfigRestController controller = new AnalyzerPluginConfigRestController();
            ReflectionTestUtils.setField(controller, "analyzerPluginConfigService", analyzerPluginConfigService);
            ReflectionTestUtils.setField(controller, "analyzerPendingCodeService", analyzerPendingCodeService);
            return controller;
        }
    }

    @Service
    static class NoOpAnalyzerPluginConfigService extends BaseObjectServiceImpl<AnalyzerPluginConfig, String>
            implements AnalyzerPluginConfigService {

        NoOpAnalyzerPluginConfigService() {
            super(AnalyzerPluginConfig.class);
        }

        @Override
        protected BaseDAO<AnalyzerPluginConfig, String> getBaseObjectDAO() {
            return mock(AnalyzerPluginConfigDAO.class);
        }

        @Override
        public AnalyzerPluginConfig getOrCreate(String analyzerId, String sysUserId) {
            return new AnalyzerPluginConfig();
        }

        @Override
        public AnalyzerPluginConfig upsert(String analyzerId, Map<String, Object> config, String sysUserId) {
            return new AnalyzerPluginConfig();
        }

        @Override
        public Map<String, Object> getConfigAsMap(String analyzerId) {
            return Map.of("connectionRole", "SERVER");
        }

        @Override
        public void applyConfigDefaults(String analyzerId, Object configDefaults, String sysUserId) {
        }

        @Override
        public boolean hasAtLeastOneActiveQcRule(String analyzerId) {
            return false;
        }
    }
}
