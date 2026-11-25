package org.openelisglobal.test.controller.rest;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
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
 * Security slice tests for {@link TestRestController}.
 * Verifies that endpoints require RECEPTION or ADMIN roles (issue #3966).
 */
@WebAppConfiguration
@ContextConfiguration(classes = { TestRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class TestRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void getTestSampleTypes_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/test-sample-types")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getTestSampleTypes_resultsReturns403() throws Exception {
        mockMvc.perform(get("/rest/test-sample-types").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getTestSampleTypes_validationReturns403() throws Exception {
        mockMvc.perform(get("/rest/test-sample-types").with(user("validation").roles("VALIDATION")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getTestSampleTypes_receptionPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/test-sample-types").with(user("reception").roles("RECEPTION")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void getTestSampleTypes_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/test-sample-types").with(user("admin").roles("ADMIN")))
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
        TestService testService() {
            return mock(TestService.class);
        }

        @Bean
        TypeOfSampleService typeOfSampleService() {
            return mock(TypeOfSampleService.class);
        }

        @Bean
        TypeOfSampleTestService typeOfSampleTestService() {
            return mock(TypeOfSampleTestService.class);
        }

        @Bean
        TypeOfSamplePanelService typeOfSamplePanelService() {
            return mock(TypeOfSamplePanelService.class);
        }

        @Bean
        TestRestController testRestController() {
            return new TestRestController();
        }
    }
}
