package org.openelisglobal.shipment.controller.rest;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.shipment.service.UnassignedSampleItemService;
import org.openelisglobal.shipment.service.UnassignedSampleService;
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
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Security slice tests for {@link UnassignedSampleRestController}. Verifies
 * that endpoints require RECEPTION, RESULTS, or ADMIN roles (issue #3966).
 */
@WebAppConfiguration
@ContextConfiguration(classes = { UnassignedSampleRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class UnassignedSampleRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static UserSessionData sessionData() {
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(1);
        return sessionData;
    }

    // --- GET /items ---

    @Test
    public void getAllUnassignedSampleItems_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample/items")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getAllUnassignedSampleItems_disallowedRoleReturns403() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample/items").with(user("validation").roles("VALIDATION")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getAllUnassignedSampleItems_receptionPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample/items").with(user("reception").roles("RECEPTION")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void getAllUnassignedSampleItems_resultsPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample/items").with(user("results").roles("RESULTS")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void getAllUnassignedSampleItems_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample/items").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    // --- GET /items/search ---

    @Test
    public void searchUnassignedSampleItems_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample/items/search").param("accessionNumber", "123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void searchUnassignedSampleItems_disallowedRoleReturns403() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample/items/search").param("accessionNumber", "123")
                .with(user("validation").roles("VALIDATION"))).andExpect(status().isForbidden());
    }

    @Test
    public void searchUnassignedSampleItems_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample/items/search").param("accessionNumber", "123")
                .with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    // --- GET /items/by-facility/{facilityId} ---

    @Test
    public void getUnassignedSampleItemsByFacility_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample/items/by-facility/1")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getUnassignedSampleItemsByFacility_disallowedRoleReturns403() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample/items/by-facility/1").with(user("validation").roles("VALIDATION")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getUnassignedSampleItemsByFacility_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample/items/by-facility/1").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    // --- GET / (deprecated) ---

    @Test
    public void getAllUnassignedSamples_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getAllUnassignedSamples_disallowedRoleReturns403() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample").with(user("validation").roles("VALIDATION")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getAllUnassignedSamples_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    // --- GET /by-facility/{facilityId} ---

    @Test
    public void getUnassignedSamplesByFacility_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample/by-facility/1")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getUnassignedSamplesByFacility_disallowedRoleReturns403() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample/by-facility/1").with(user("validation").roles("VALIDATION")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getUnassignedSamplesByFacility_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample/by-facility/1").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    // --- GET /count-by-facility/{facilityId} ---

    @Test
    public void countUnassignedSamplesByFacility_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample/count-by-facility/1")).andExpect(status().isUnauthorized());
    }

    @Test
    public void countUnassignedSamplesByFacility_disallowedRoleReturns403() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample/count-by-facility/1").with(user("validation").roles("VALIDATION")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void countUnassignedSamplesByFacility_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/unassigned-sample/count-by-facility/1").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    // --- PUT /{referralId}/assign-to-box ---

    @Test
    public void assignSampleToBox_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(put("/rest/unassigned-sample/1/assign-to-box").contentType(MediaType.APPLICATION_JSON)
                .content("{\"boxId\":\"box-1\"}").sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void assignSampleToBox_disallowedRoleReturns403() throws Exception {
        mockMvc.perform(put("/rest/unassigned-sample/1/assign-to-box").contentType(MediaType.APPLICATION_JSON)
                .content("{\"boxId\":\"box-1\"}").sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData())
                .with(user("validation").roles("VALIDATION"))).andExpect(status().isForbidden());
    }

    @Test
    public void assignSampleToBox_receptionPassesAuthorization() throws Exception {
        mockMvc.perform(put("/rest/unassigned-sample/1/assign-to-box").contentType(MediaType.APPLICATION_JSON)
                .content("{\"boxId\":\"box-1\"}").sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData())
                .with(user("reception").roles("RECEPTION")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void assignSampleToBox_resultsPassesAuthorization() throws Exception {
        mockMvc.perform(put("/rest/unassigned-sample/1/assign-to-box").contentType(MediaType.APPLICATION_JSON)
                .content("{\"boxId\":\"box-1\"}").sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData())
                .with(user("results").roles("RESULTS")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void assignSampleToBox_adminPassesAuthorization() throws Exception {
        mockMvc.perform(put("/rest/unassigned-sample/1/assign-to-box").contentType(MediaType.APPLICATION_JSON)
                .content("{\"boxId\":\"box-1\"}").sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData())
                .with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    // --- PUT /{referralId}/mark-lost ---

    @Test
    public void markSampleAsLost_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(put("/rest/unassigned-sample/1/mark-lost").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"broken\"}").sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void markSampleAsLost_disallowedRoleReturns403() throws Exception {
        mockMvc.perform(put("/rest/unassigned-sample/1/mark-lost").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"broken\"}").sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData())
                .with(user("validation").roles("VALIDATION"))).andExpect(status().isForbidden());
    }

    @Test
    public void markSampleAsLost_adminPassesAuthorization() throws Exception {
        mockMvc.perform(put("/rest/unassigned-sample/1/mark-lost").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"broken\"}").sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData())
                .with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    // --- PUT /{referralId}/cancel ---

    @Test
    public void cancelReferral_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(put("/rest/unassigned-sample/1/cancel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"cancelled\"}").sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void cancelReferral_disallowedRoleReturns403() throws Exception {
        mockMvc.perform(put("/rest/unassigned-sample/1/cancel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"cancelled\"}").sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData())
                .with(user("validation").roles("VALIDATION"))).andExpect(status().isForbidden());
    }

    @Test
    public void cancelReferral_adminPassesAuthorization() throws Exception {
        mockMvc.perform(put("/rest/unassigned-sample/1/cancel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"cancelled\"}").sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData())
                .with(user("admin").roles("ADMIN")))
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
        UnassignedSampleService unassignedSampleService() {
            return mock(UnassignedSampleService.class);
        }

        @Bean
        UnassignedSampleItemService unassignedSampleItemService() {
            return mock(UnassignedSampleItemService.class);
        }

        @Bean
        UnassignedSampleRestController unassignedSampleRestController() {
            return new UnassignedSampleRestController();
        }
    }
}
