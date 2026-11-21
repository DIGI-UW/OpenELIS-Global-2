package org.openelisglobal.notification.controller.rest;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notification.service.TestNotificationConfigService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.test.service.TestService;
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
 * The Test Notification Configuration menu and its save action use this
 * controller. All are in the Global Administrator-only admin area (issue
 * #3966).
 */
@WebAppConfiguration
@ContextConfiguration(classes = { TestNotificationConfigMenuRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class TestNotificationConfigMenuRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void displayNotificationConfig_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/TestNotificationConfigMenu")).andExpect(status().isUnauthorized());
    }

    @Test
    public void displayNotificationConfig_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/TestNotificationConfigMenu").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void displayNotificationConfig_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/TestNotificationConfigMenu").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void displayNotificationConfig_receptionReturns403() throws Exception {
        mockMvc.perform(get("/rest/TestNotificationConfigMenu").with(user("reception").roles("RECEPTION")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void updateNotificationConfig_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post("/rest/TestNotificationConfigMenu").contentType(MediaType.APPLICATION_JSON)
                .content("{\"menuList\":[]}")).andExpect(status().isUnauthorized());
    }

    @Test
    public void updateNotificationConfig_nonAdminReturns403() throws Exception {
        mockMvc.perform(post("/rest/TestNotificationConfigMenu").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"menuList\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void updateNotificationConfig_adminPassesAuthorization() throws Exception {
        mockMvc.perform(post("/rest/TestNotificationConfigMenu").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"menuList\":[]}")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, adminSessionData()))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    private static UserSessionData adminSessionData() {
        // The save handler also needs the legacy OpenELIS audit identity.
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(1);
        return sessionData;
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
        TestNotificationConfigService testNotificationConfigService() {
            return mock(TestNotificationConfigService.class);
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
        TestNotificationConfigMenuRestController testNotificationConfigMenuRestController() {
            return new TestNotificationConfigMenuRestController();
        }
    }
}
