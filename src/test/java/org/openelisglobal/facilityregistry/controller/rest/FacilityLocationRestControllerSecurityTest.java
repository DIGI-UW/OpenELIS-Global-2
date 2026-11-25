package org.openelisglobal.facilityregistry.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.facilityregistry.form.FacilityLocationStatus;
import org.openelisglobal.facilityregistry.service.FacilityLocationModeException;
import org.openelisglobal.facilityregistry.service.FacilityRegistryService;
import org.openelisglobal.login.valueholder.UserSessionData;
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
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = { FacilityLocationRestControllerSecurityTest.TestConfig.class })
public class FacilityLocationRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String URL = "/rest/facility-location";

    @Test
    public void readingWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    public void readingAsNonAdminReturns403() throws Exception {
        mockMvc.perform(get(URL).with(user("results").roles("RESULTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void readingAsAdminReturnsTheStatus() throws Exception {
        mockMvc.perform(get(URL).with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("AUTO")).andExpect(jsonPath("$.source").value("ip"));
    }

    @Test
    public void postingWithoutACsrfTokenIsRefused() throws Exception {
        mockMvc.perform(post(URL).with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\":0.05,\"longitude\":32.46}")).andExpect(status().isForbidden());
    }

    @Test
    public void postingAsNonAdminIsRefused() throws Exception {
        mockMvc.perform(post(URL).with(user("results").roles("RESULTS")).with(csrf())
                .requestAttr(IActionConstants.USER_SESSION_DATA, adminSession()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\":0.05,\"longitude\":32.46}")).andExpect(status().isForbidden());
    }

    @Test
    public void postingABrowserPositionAsAdminStoresIt() throws Exception {
        mockMvc.perform(post(URL).with(user("admin").roles("ADMIN")).with(csrf())
                .requestAttr(IActionConstants.USER_SESSION_DATA, adminSession()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\":0.05,\"longitude\":32.46}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("browser"));
    }

    @Test
    public void postingInManualModeReturns409WithTheReason() throws Exception {
        mockMvc.perform(post(URL).with(user("admin").roles("ADMIN")).with(csrf())
                .requestAttr(IActionConstants.USER_SESSION_DATA, adminSession()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\":7,\"longitude\":7}")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The facility location is in manual mode"));
    }

    @Test
    public void postingAnOutOfRangePositionReturns400() throws Exception {
        mockMvc.perform(post(URL).with(user("admin").roles("ADMIN")).with(csrf())
                .requestAttr(IActionConstants.USER_SESSION_DATA, adminSession()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\":91,\"longitude\":7}")).andExpect(status().isBadRequest());
    }

    private static UserSessionData adminSession() {
        UserSessionData session = new UserSessionData();
        session.setSytemUserId(1);
        return session;
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).httpBasic(Customizer.withDefaults());
            return http.build();
        }

        @Bean
        FacilityRegistryService facilityRegistryService() {
            FacilityRegistryService service = mock(FacilityRegistryService.class);
            FacilityLocationStatus current = new FacilityLocationStatus();
            current.setMode("AUTO");
            current.setSource("ip");
            when(service.getStatus()).thenReturn(current);
            FacilityLocationStatus browser = new FacilityLocationStatus();
            browser.setMode("AUTO");
            browser.setSource("browser");
            when(service.recordBrowserLocation(any(), any(), any())).thenReturn(browser);
            when(service.recordBrowserLocation(argThat(v -> v != null && v.compareTo(BigDecimal.valueOf(7)) == 0),
                    any(), any()))
                    .thenThrow(new FacilityLocationModeException("The facility location is in manual mode"));
            when(service.recordBrowserLocation(argThat(v -> v != null && v.compareTo(BigDecimal.valueOf(91)) == 0),
                    any(), any())).thenThrow(new IllegalArgumentException("Latitude must be between -90 and 90"));
            return service;
        }

        @Bean
        FacilityLocationRestController facilityLocationRestController() {
            return new FacilityLocationRestController();
        }
    }
}
