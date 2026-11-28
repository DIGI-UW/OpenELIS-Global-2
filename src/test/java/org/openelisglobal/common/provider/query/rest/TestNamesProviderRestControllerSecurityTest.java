package org.openelisglobal.common.provider.query.rest;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
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
 * Security slice tests for {@link TestNamesProviderRestController}. Verifies
 * that the endpoint requires ADMIN role (issue #3966).
 */
@WebAppConfiguration
@ContextConfiguration(classes = { TestNamesProviderRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class TestNamesProviderRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void processRequest_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/TestNamesProvider").param("testId", "1")).andExpect(status().isUnauthorized());
    }

    @Test
    public void processRequest_receptionReturns403() throws Exception {
        mockMvc.perform(get("/rest/TestNamesProvider").param("testId", "1").with(user("reception").roles("RECEPTION")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void processRequest_resultsReturns403() throws Exception {
        mockMvc.perform(get("/rest/TestNamesProvider").param("testId", "1").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void processRequest_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/TestNamesProvider").param("testId", "1").with(user("admin").roles("ADMIN")))
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
        SpringContext springContext(ApplicationContext context) {
            SpringContext sc = new SpringContext();
            sc.setApplicationContext(context);
            return sc;
        }

        @Bean
        @DependsOn("springContext")
        TestNamesProviderRestController testNamesProviderRestController(TestService testService) {
            return new TestNamesProviderRestController(testService);
        }
    }
}
