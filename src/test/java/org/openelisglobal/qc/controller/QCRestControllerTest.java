package org.openelisglobal.qc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.qc.builder.QCControlLotBuilder;
import org.openelisglobal.qc.dto.InstrumentQCStatus;
import org.openelisglobal.qc.dto.QCDashboardSummary;
import org.openelisglobal.qc.dto.RuleConfigSummary;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.service.QCDashboardService;
import org.openelisglobal.qc.service.QCStatisticsService;
import org.openelisglobal.qc.service.WestgardRuleConfigService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Controller-layer unit tests for {@link QCRestController}.
 *
 * <p>
 * These are standalone MockMvc tests — they exercise the web layer (URL
 * routing, path/query binding, HTTP status mapping, JSON serialization) against
 * mocked services. They do NOT boot the Spring context or touch the database.
 *
 * <p>
 * Service-layer logic (e.g. control lot lifecycle) is covered separately in
 * {@code QCControlLotServiceTest}. End-to-end flow is covered by the analyzer
 * harness E2E run (T001–T004) and the Playwright dashboard smoke (T008–T010).
 */
@RunWith(MockitoJUnitRunner.class)
public class QCRestControllerTest {

    @Mock
    private QCControlLotService controlLotService;

    @Mock
    private QCStatisticsService statisticsService;

    @Mock
    private WestgardRuleConfigService ruleConfigService;

    @Mock
    private QCDashboardService dashboardService;

    @InjectMocks
    private QCRestController controller;

    private MockMvc mockMvc;
    private QCControlLot sampleLot;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        sampleLot = QCControlLotBuilder.create().withId("lot-1").withLotNumber("LOT-2026-001").withTestId("42")
                .withInstrumentId("7").asActive().build();
    }

    // ==================== Control lot retrieval ====================

    @Test
    public void getAllControlLots_returnsServicePayload() throws Exception {
        when(controlLotService.getAllControlLots()).thenReturn(Arrays.asList(sampleLot));

        mockMvc.perform(get("/rest/qc/control-lots")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("lot-1")).andExpect(jsonPath("$[0].lotNumber").value("LOT-2026-001"));

        verify(controlLotService, times(1)).getAllControlLots();
    }

    @Test
    public void getActiveControlLots_bindsRequestParams() throws Exception {
        when(controlLotService.getActiveControlLots("42", "7")).thenReturn(Arrays.asList(sampleLot));

        mockMvc.perform(get("/rest/qc/controlLots").param("testId", "42").param("instrumentId", "7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("lot-1"));

        verify(controlLotService).getActiveControlLots("42", "7");
    }

    @Test
    public void getControlLot_returns200WhenFound() throws Exception {
        when(controlLotService.get("lot-1")).thenReturn(sampleLot);

        mockMvc.perform(get("/rest/qc/controlLot/lot-1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("lot-1"));
    }

    @Test
    public void getControlLot_returns404WhenNull() throws Exception {
        when(controlLotService.get("missing")).thenReturn(null);

        mockMvc.perform(get("/rest/qc/controlLot/missing")).andExpect(status().isNotFound());
    }

    @Test
    public void getControlLot_returns500OnServiceException() throws Exception {
        when(controlLotService.get(eq("boom"))).thenThrow(new RuntimeException("db down"));

        mockMvc.perform(get("/rest/qc/controlLot/boom")).andExpect(status().isInternalServerError());
    }

    // ==================== Lifecycle transitions ====================

    @Test
    public void activateControlLot_returns200WhenTransitioned() throws Exception {
        QCControlLot active = QCControlLotBuilder.create().withId("lot-1").asActive().build();
        when(controlLotService.activateControlLot("lot-1")).thenReturn(active);

        mockMvc.perform(put("/rest/qc/controlLot/lot-1/activate")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    public void activateControlLot_returns404WhenLotMissing() throws Exception {
        when(controlLotService.activateControlLot("nope")).thenReturn(null);

        mockMvc.perform(put("/rest/qc/controlLot/nope/activate")).andExpect(status().isNotFound());
    }

    // ==================== Dashboard ====================

    @Test
    public void getDashboardSummary_defaultMonthsParam() throws Exception {
        QCDashboardSummary summary = new QCDashboardSummary();
        when(dashboardService.getDashboardSummary(any(Timestamp.class), any(Timestamp.class))).thenReturn(summary);

        mockMvc.perform(get("/rest/qc/dashboard/summary")).andExpect(status().isOk());

        verify(dashboardService).getDashboardSummary(any(Timestamp.class), any(Timestamp.class));
    }

    @Test
    public void getAllInstrumentQCStatus_returnsEmptyListWhenNoInstruments() throws Exception {
        when(dashboardService.getAllInstrumentComplianceStatus(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(Collections.<InstrumentQCStatus>emptyList());

        mockMvc.perform(get("/rest/qc/dashboard/instruments").param("months", "3")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void getInstrumentQCStatus_returns200ForInstrument() throws Exception {
        InstrumentQCStatus status = new InstrumentQCStatus();
        when(dashboardService.getInstrumentComplianceStatus(eq("23"), any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(status);

        mockMvc.perform(get("/rest/qc/dashboard/instruments/23")).andExpect(status().isOk());

        verify(dashboardService).getInstrumentComplianceStatus(eq("23"), any(Timestamp.class), any(Timestamp.class));
    }

    // ==================== Rule configuration ====================

    @Test
    public void getAllRuleConfigSummaries_returnsServicePayload() throws Exception {
        when(ruleConfigService.getAllRuleConfigSummaries()).thenReturn(Collections.<RuleConfigSummary>emptyList());

        mockMvc.perform(get("/rest/qc/ruleConfig/summaries")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void getRuleConfigurations_bindsTestAndInstrument() throws Exception {
        when(ruleConfigService.findByTestAndInstrument(anyString(), anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/rest/qc/ruleConfig").param("testId", "42").param("instrumentId", "7"))
                .andExpect(status().isOk());

        verify(ruleConfigService).findByTestAndInstrument("42", "7");
    }
}
