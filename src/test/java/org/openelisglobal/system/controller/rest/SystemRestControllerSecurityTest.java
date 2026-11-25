package org.openelisglobal.system.controller.rest;

import static org.junit.Assert.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
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

/**
 * Order entry and Non-Conforming Events pages use the shared laboratory clock.
 * Server time is available to every authenticated role (issue #3966).
 */
@WebAppConfiguration
@ContextConfiguration(classes = { SystemRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class SystemRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void getServerTime_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/server-time")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getServerTime_resultsPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/server-time").with(user("results").roles("RESULTS")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
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
        SystemRestController systemRestController() {
            return new SystemRestController();
        }
    }
}
