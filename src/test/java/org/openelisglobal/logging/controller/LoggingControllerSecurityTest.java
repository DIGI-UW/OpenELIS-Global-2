package org.openelisglobal.logging.controller;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.logging.controller.LoggingController.InMemoryLogAppender;
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
@ContextConfiguration(classes = { LoggingControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class LoggingControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void testLoggingChange_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/logging")).andExpect(status().isUnauthorized());
    }

    @Test
    public void testLoggingChange_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(get("/logging").with(user("results").roles("RESULTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void testLoggingChange_AdminRole_Returns200() throws Exception {
        mockMvc.perform(get("/logging").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
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
        LoggingController loggingController() {
            return new LoggingController();
        }

        @Bean
        InMemoryLogAppender inMemoryLogAppender() {
            return mock(InMemoryLogAppender.class);
        }
    }
}
