package org.openelisglobal.common.controller;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.common.service.DatabaseCleanService;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@ContextConfiguration(classes = { DatabaseCleaningRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class DatabaseCleaningRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void testStatus_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/rest/database-cleaning/status")).andExpect(status().isUnauthorized());
    }

    @Test
    public void testStatus_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(get("/rest/database-cleaning/status").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCleanDatabase_AdminRole_Returns403() throws Exception {
        mockMvc.perform(post("/rest/database-cleaning").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
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
        DatabaseCleanService databaseCleanService() {
            return mock(DatabaseCleanService.class);
        }

        @Bean
        HistoryService historyService() {
            return mock(HistoryService.class);
        }

        @Bean
        DatabaseCleaningRestController databaseCleaningRestController() {
            return new DatabaseCleaningRestController();
        }
    }
}
