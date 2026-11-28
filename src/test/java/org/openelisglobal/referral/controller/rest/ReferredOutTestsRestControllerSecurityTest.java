package org.openelisglobal.referral.controller.rest;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.referral.service.ReferralService;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Security slice tests for {@link ReferredOutTestsRestController}. Verifies
 * that endpoints require RECEPTION, RESULTS, VALIDATION, or ADMIN roles (issue
 * #3966).
 */
@WebAppConfiguration
@ContextConfiguration(classes = { ReferredOutTestsRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class ReferredOutTestsRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private DisplayListService previousDisplayListService;

    // The form loads its select lists from the static DisplayListService instance.
    @Before
    public void useMockDisplayLists() {
        previousDisplayListService = DisplayListService.getInstance();
        ReflectionTestUtils.setField(DisplayListService.class, "instance", mock(DisplayListService.class));
    }

    @After
    public void restoreDisplayLists() {
        ReflectionTestUtils.setField(DisplayListService.class, "instance", previousDisplayListService);
    }

    @Test
    public void showReferredOutTests_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/ReferredOutTests")).andExpect(status().isUnauthorized());
    }

    @Test
    public void showReferredOutTests_disallowedRoleReturns403() throws Exception {
        mockMvc.perform(get("/rest/ReferredOutTests").with(user("billing").roles("BILLING")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void showReferredOutTests_receptionPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/ReferredOutTests").with(user("reception").roles("RECEPTION")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void showReferredOutTests_resultsPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/ReferredOutTests").with(user("results").roles("RESULTS")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void showReferredOutTests_validationPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/ReferredOutTests").with(user("validation").roles("VALIDATION")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void showReferredOutTests_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/ReferredOutTests").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestConfig implements WebMvcConfigurer {

        @Override
        public Validator getValidator() {
            return new Validator() {
                @Override
                public boolean supports(Class<?> clazz) {
                    return true;
                }

                @Override
                public void validate(Object target, Errors errors) {
                }
            };
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).httpBasic(Customizer.withDefaults())
                    .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean
        ReferralService referralService() {
            return mock(ReferralService.class);
        }

        @Bean
        ReferredOutTestsRestController referredOutTestsRestController() {
            return new ReferredOutTestsRestController();
        }
    }
}
