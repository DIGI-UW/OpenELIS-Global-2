package org.openelisglobal.configuration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.configuration.service.ConfigurationInitializationService;
import org.openelisglobal.configuration.service.ConfigurationReloadFileResult;
import org.openelisglobal.configuration.service.ConfigurationReloadOptions;
import org.openelisglobal.configuration.service.ConfigurationReloadResult;
import org.openelisglobal.security.DaemonContextExecutor;
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

@WebAppConfiguration
@ContextConfiguration(classes = { ConfigurationReloadRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class ConfigurationReloadRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void reloadDomains_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post("/rest/configuration/domains/reload").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void reloadDomains_nonAdminReturns403() throws Exception {
        mockMvc.perform(post("/rest/configuration/domains/reload").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void reloadDomains_adminReturns200() throws Exception {
        mockMvc.perform(post("/rest/configuration/domains/reload").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"domains\":[\"roles\"],\"force\":true}"))
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

        // Required: Spring autowires @Autowired fields on @Bean-returned mocks
        // (Mockito proxies still carry the parent class's @Autowired metadata).
        // The mocked ConfigurationInitializationService has an @Autowired
        // DaemonContextExecutor field that must resolve, so DaemonContextExecutor
        // (and its @Qualifier("daemonSysUserId") dependency) must be present here.
        @Bean("daemonSysUserId")
        String daemonSysUserId() {
            return "1";
        }

        @Bean
        DaemonContextExecutor daemonContextExecutor() {
            return new DaemonContextExecutor();
        }

        @Bean
        ConfigurationInitializationService configurationInitializationService() {
            ConfigurationInitializationService service = mock(ConfigurationInitializationService.class);
            when(service.reload(any(ConfigurationReloadOptions.class))).thenReturn(new ConfigurationReloadResult(
                    List.of(ConfigurationReloadFileResult.processed("roles", "roles.csv"))));
            return service;
        }

        @Bean
        ConfigurationReloadRefreshService configurationReloadRefreshService() {
            return mock(ConfigurationReloadRefreshService.class);
        }

        @Bean
        ConfigurationReloadRestController configurationReloadRestController() {
            return new ConfigurationReloadRestController();
        }
    }
}
