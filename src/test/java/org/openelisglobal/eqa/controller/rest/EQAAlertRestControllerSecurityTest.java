package org.openelisglobal.eqa.controller.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = { EQAAlertRestControllerSecurityTest.TestConfig.class })
public class EQAAlertRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void summary_globalAdmin_seesTheOpenAlerts() throws Exception {
        mockMvc.perform(get("/rest/alerts/dashboard/summary").with(user("ga").roles("GLOBAL_ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalOpen").value(1));
    }

    @Test
    public void summary_reception_seesTheOpenAlerts() throws Exception {
        mockMvc.perform(get("/rest/alerts/dashboard/summary").with(user("r").roles("RECEPTION")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalOpen").value(1));
    }

    @Test
    public void summary_reportsOnly_isForbidden() throws Exception {
        mockMvc.perform(get("/rest/alerts/dashboard/summary").with(user("rep").roles("REPORTS")))
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
        AlertService alertService() {
            Alert lowStock = new Alert();
            lowStock.setId(1L);
            lowStock.setAlertType(AlertType.INVENTORY_LOW);
            lowStock.setSeverity(AlertSeverity.WARNING);
            lowStock.setStatus(AlertStatus.OPEN);
            lowStock.setStartTime(OffsetDateTime.now());
            lowStock.setMessage("Low stock");
            AlertService service = mock(AlertService.class);
            when(service.getAll()).thenReturn(List.of(lowStock));
            return service;
        }

        @Bean
        EQAAlertRestController eqaAlertRestController() {
            return new EQAAlertRestController();
        }
    }
}
