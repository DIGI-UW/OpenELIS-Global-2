package org.openelisglobal.alert.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Role assertions for /rest/alerts. This controller is shared: the Cold Storage
 * dashboard (RECEPTION/ADMIN) and CriticalBanner on the unified results and
 * validation pages (RESULTS/VALIDATION) both read it, so the read has to admit
 * all four while DELETE stays ADMIN-only and freezer-only.
 *
 * <p>
 * BaseWebContextSensitiveTest cannot see any of this: its AppTestConfig
 * excludes SecurityConfig, so @EnableMethodSecurity never activates and every
 * 
 * @PreAuthorize is inert there.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { AlertRestControllerSecurityTest.TestConfig.class })
public class AlertRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final long FREEZER_ALERT_ID = 11L;
    private static final long ANALYSIS_ALERT_ID = 22L;

    @Test
    public void getAlerts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/rest/alerts").param("entityType", "Freezer")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getAlerts_receptionRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/alerts").param("entityType", "Freezer").with(user("r").roles("RECEPTION")))
                .andExpect(status().isOk());
    }

    @Test
    public void getAlerts_resultsRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/alerts").param("entityType", "ANALYSIS").param("entityId", "5")
                .with(user("res").roles("RESULTS"))).andExpect(status().isOk());
    }

    @Test
    public void getAlerts_validationRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/alerts").param("entityType", "ANALYSIS").param("entityId", "5")
                .with(user("val").roles("VALIDATION"))).andExpect(status().isOk());
    }

    @Test
    public void getAlerts_adminRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/alerts").param("entityType", "Freezer").with(user("a").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    public void getAlerts_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(get("/rest/alerts").param("entityType", "Freezer").with(user("rep").roles("REPORTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getAlerts_byEntityTypeAlone_returnsOnlyThatEntityTypesAlerts() throws Exception {
        mockMvc.perform(get("/rest/alerts").param("entityType", "Freezer").with(user("a").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].alertType").value("FREEZER_TEMPERATURE"));
    }

    @Test
    public void acknowledgeAlert_resultsRole_returns403() throws Exception {
        mockMvc.perform(put("/rest/alerts/" + FREEZER_ALERT_ID + "/acknowledge").with(user("res").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"n\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void deleteAlert_receptionRole_returns403() throws Exception {
        mockMvc.perform(delete("/rest/alerts/" + FREEZER_ALERT_ID).with(user("r").roles("RECEPTION")))
                .andExpect(status().isForbidden());
    }

    /**
     * ADMIN clears the alert reached from the Cold Storage list.
     */
    @Test
    public void deleteAlert_adminRole_onFreezerAlert_returns204() throws Exception {
        mockMvc.perform(
                delete("/rest/alerts/" + FREEZER_ALERT_ID).session(sessionForSysUser(1)).with(user("a").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    /**
     * The same ADMIN must not be able to hard-delete another module's alert through
     * the endpoint the Cold Storage dashboard opened.
     */
    @Test
    public void deleteAlert_adminRole_onNonFreezerAlert_returns403() throws Exception {
        mockMvc.perform(delete("/rest/alerts/" + ANALYSIS_ALERT_ID).session(sessionForSysUser(1))
                .with(user("a").roles("ADMIN"))).andExpect(status().isForbidden());
    }

    /**
     * ControllerUtills.getSysUserId falls back to a SpringContext bean this slice
     * has no context for, so hand it the session attribute the login flow sets.
     */
    private static MockHttpSession sessionForSysUser(int sysUserId) {
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(sysUserId);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return session;
    }

    private static Alert alert(Long id, AlertType type, String entityType, Long entityId) {
        Alert alert = new Alert();
        alert.setId(id);
        alert.setAlertType(type);
        alert.setAlertEntityType(entityType);
        alert.setAlertEntityId(entityId);
        alert.setSeverity(AlertSeverity.CRITICAL);
        alert.setStatus(AlertStatus.OPEN);
        alert.setStartTime(OffsetDateTime.now());
        alert.setMessage("m");
        return alert;
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
        AlertService alertService() {
            Alert freezerAlert = alert(FREEZER_ALERT_ID, AlertType.FREEZER_TEMPERATURE, "Freezer", 100L);
            Alert analysisAlert = alert(ANALYSIS_ALERT_ID, AlertType.CRITICAL_RESULT, "ANALYSIS", 777L);

            AlertService service = mock(AlertService.class);
            when(service.getAll()).thenReturn(List.of(freezerAlert, analysisAlert));
            when(service.getAlertsByEntity(eq("Freezer"), isNull())).thenReturn(List.of(freezerAlert));
            when(service.getAlertsByEntity(eq("ANALYSIS"), anyLong())).thenReturn(Collections.emptyList());
            when(service.get(FREEZER_ALERT_ID)).thenReturn(freezerAlert);
            when(service.get(ANALYSIS_ALERT_ID)).thenReturn(analysisAlert);
            return service;
        }

        @Bean
        FreezerService freezerService() {
            FreezerService service = mock(FreezerService.class);
            when(service.findById(any())).thenReturn(Optional.empty());
            return service;
        }

        @Bean
        AlertRestController alertRestController() {
            return new AlertRestController();
        }
    }
}
