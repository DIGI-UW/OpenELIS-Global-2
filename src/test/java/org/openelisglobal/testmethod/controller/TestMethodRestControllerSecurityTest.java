package org.openelisglobal.testmethod.controller;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.testmethod.controller.rest.TestMethodRestController;
import org.openelisglobal.testmethod.service.TestMethodService;
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

/**
 * The TestMethod link API (/rest/test/{testId}/methods) is gated on ROLE_ADMIN:
 * 401 for the unauthenticated, 403 for a non-admin, and the controller is
 * reached (200) for an admin. Auth ordering is asserted before any business
 * behavior so a future refactor that drops the @PreAuthorize is caught here.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { TestMethodRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class TestMethodRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void getLinkedMethods_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/test/1/methods")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getLinkedMethods_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/test/1/methods").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getLinkedMethods_adminReachesController() throws Exception {
        // Admin passes the gate; the mocked service returns an empty list -> 200,
        // proving the request reached the controller rather than being blocked by auth.
        mockMvc.perform(get("/rest/test/1/methods").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }

    // Write endpoints share the class-level @PreAuthorize; cover them so a later
    // move to per-method annotations that misses one is caught here.

    @Test
    public void linkMethod_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post("/rest/test/1/methods").contentType(MediaType.APPLICATION_JSON)
                .content("{\"methodId\":\"1\",\"effectiveDate\":\"2026-01-01\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    public void updateLink_nonAdminReturns403() throws Exception {
        mockMvc.perform(patch("/rest/test/1/methods/abc").contentType(MediaType.APPLICATION_JSON).content("{}")
                .with(user("results").roles("RESULTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void removeLink_nonAdminReturns403() throws Exception {
        mockMvc.perform(delete("/rest/test/1/methods/abc").with(user("results").roles("RESULTS")))
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
        TestMethodService testMethodService() {
            return mock(TestMethodService.class);
        }

        @Bean
        UserModuleService userModuleService() {
            return mock(UserModuleService.class);
        }

        @Bean
        PageBuilderService pageBuilderService() {
            return mock(PageBuilderService.class);
        }

        @Bean
        TestMethodRestController testMethodRestController() {
            return new TestMethodRestController();
        }
    }
}
