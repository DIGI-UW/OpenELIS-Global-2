package org.openelisglobal.coldstorage.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.coldstorage.service.CorrectiveActionService;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Slice test for {@link FreezerAuditTrailController}: a request carrying only
 * {@code start} or only {@code end} must still filter, and must not 500.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { FreezerAuditTrailControllerSecurityTest.TestConfig.class })
public class FreezerAuditTrailControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final long FREEZER_ID = 42L;
    private static final String BASE = "/rest/coldstorage/audit-trail";

    private static final OffsetDateTime T0 = OffsetDateTime.parse("2025-01-01T00:00:00Z");
    private static final OffsetDateTime T1 = OffsetDateTime.parse("2025-01-15T00:00:00Z");
    private static final OffsetDateTime T2 = OffsetDateTime.parse("2025-02-01T00:00:00Z");

    @Autowired
    private HistoryService historyService;
    @Autowired
    private FreezerService freezerService;
    @Autowired
    private ReferenceTablesService referenceTablesService;
    @Autowired
    private AlertService alertService;
    @Autowired
    private CorrectiveActionService correctiveActionService;

    @Before
    public void setUpMocks() {
        Freezer freezer = new Freezer();
        freezer.setId(FREEZER_ID);
        freezer.setName("Freezer A");
        when(freezerService.findById(FREEZER_ID)).thenReturn(Optional.of(freezer));
        when(freezerService.getAllFreezersForReporting()).thenReturn(List.of(freezer));

        ReferenceTables freezerTable = new ReferenceTables();
        freezerTable.setId("1");
        ReferenceTables caTable = new ReferenceTables();
        caTable.setId("2");
        when(referenceTablesService.getReferenceTableByName("FREEZER")).thenReturn(freezerTable);
        when(referenceTablesService.getReferenceTableByName("CORRECTIVE_ACTION")).thenReturn(caTable);

        when(historyService.getHistoryByRefIdAndRefTableId(anyString(), anyString())).thenReturn(List.of());
        when(alertService.getAlertsByEntity(anyString(), any())).thenReturn(List.of());
        when(correctiveActionService.getCorrectiveActionsByFreezerId(any())).thenReturn(List.of());
    }

    @Test
    public void testGetAuditTrail_StartOnly_FiltersHistory() throws Exception {
        when(historyService.getHistoryByRefIdAndRefTableId(eq(String.valueOf(FREEZER_ID)), anyString()))
                .thenReturn(List.of(historyAt("T0", T0), historyAt("T2", T2)));

        mockMvc.perform(get(BASE)
                        .param("freezerId", String.valueOf(FREEZER_ID))
                        .param("start", T1.toString())
                        .with(user("reception").roles("RECEPTION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("CONFIG-T2"))
                .andExpect(jsonPath("$[0].actionType").value("FREEZER_RENAMED"));
    }

    @Test
    public void testGetAuditTrail_EndOnly_FiltersHistory() throws Exception {
        when(historyService.getHistoryByRefIdAndRefTableId(eq(String.valueOf(FREEZER_ID)), anyString()))
                .thenReturn(List.of(historyAt("T0", T0), historyAt("T2", T2)));

        mockMvc.perform(get(BASE)
                        .param("freezerId", String.valueOf(FREEZER_ID))
                        .param("end", T1.toString())
                        .with(user("reception").roles("RECEPTION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("CONFIG-T0"));
    }

    @Test
    public void testGetAuditTrail_StartOnlyOnAlertAck_Returns200AndFilters() throws Exception {
        when(alertService.getAlertsByEntity("Freezer", FREEZER_ID))
                .thenReturn(List.of(acknowledgedAlertAt(1L, T0), acknowledgedAlertAt(2L, T2)));

        mockMvc.perform(get(BASE)
                        .param("freezerId", String.valueOf(FREEZER_ID))
                        .param("start", T1.toString())
                        .with(user("reception").roles("RECEPTION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("ACK-2"))
                .andExpect(jsonPath("$[0].actionType").value("ALERT_ACKNOWLEDGED"));
    }

    @Test
    public void testGetAuditTrail_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetAuditTrail_UnrelatedRole_Returns403() throws Exception {
        mockMvc.perform(get(BASE).with(user("results").roles("RESULTS"))).andExpect(status().isForbidden());
    }

    private History historyAt(String id, OffsetDateTime when) {
        History h = new History();
        h.setId(id);
        h.setTimestamp(Timestamp.from(when.toInstant()));
        h.setSysUserId("1");
        h.setChanges("<name>Freezer A</name>".getBytes(StandardCharsets.UTF_8));
        return h;
    }

    private Alert acknowledgedAlertAt(Long id, OffsetDateTime when) {
        Alert a = new Alert();
        a.setId(id);
        a.setAcknowledgedAt(when);
        a.setMessage("excursion");
        return a;
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
        HistoryService historyService() {
            return mock(HistoryService.class);
        }

        @Bean
        ReferenceTablesService referenceTablesService() {
            return mock(ReferenceTablesService.class);
        }

        @Bean
        SystemUserService systemUserService() {
            return mock(SystemUserService.class);
        }

        @Bean
        FreezerService freezerService() {
            return mock(FreezerService.class);
        }

        @Bean
        AlertService alertService() {
            return mock(AlertService.class);
        }

        @Bean
        CorrectiveActionService correctiveActionService() {
            return mock(CorrectiveActionService.class);
        }

        @Bean
        FreezerAuditTrailController freezerAuditTrailController(HistoryService historyService,
                ReferenceTablesService referenceTablesService, SystemUserService systemUserService,
                FreezerService freezerService, AlertService alertService,
                CorrectiveActionService correctiveActionService) {
            FreezerAuditTrailController controller = new FreezerAuditTrailController();
            ReflectionTestUtils.setField(controller, "historyService", historyService);
            ReflectionTestUtils.setField(controller, "referenceTablesService", referenceTablesService);
            ReflectionTestUtils.setField(controller, "systemUserService", systemUserService);
            ReflectionTestUtils.setField(controller, "freezerService", freezerService);
            ReflectionTestUtils.setField(controller, "alertService", alertService);
            ReflectionTestUtils.setField(controller, "correctiveActionService", correctiveActionService);
            return controller;
        }
    }
}