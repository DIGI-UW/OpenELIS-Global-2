package org.openelisglobal.qc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.qc.form.QCViolationForm;
import org.openelisglobal.qc.service.QCRuleViolationService;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Controller-layer unit tests for {@link QCViolationRestController}.
 *
 * <p>
 * Verifies the filter-parameter branching in {@code getViolations} (the
 * controller picks a different service method depending on which query params
 * are supplied), plus ID lookup, counts, and 404 mapping. Standalone MockMvc —
 * no Spring context, no DB.
 */
@RunWith(MockitoJUnitRunner.class)
public class QCViolationRestControllerTest {

    @Mock
    private QCRuleViolationService violationService;

    @InjectMocks
    private QCViolationRestController controller;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Default stub: any violation → some form. Individual tests override as needed.
        when(violationService.toForm(any(QCRuleViolation.class))).thenAnswer(invocation -> {
            QCRuleViolation v = invocation.getArgument(0);
            QCViolationForm form = new QCViolationForm();
            form.setId(v.getId());
            form.setRuleCode(v.getRuleCode());
            form.setSeverity(v.getSeverity());
            return form;
        });
    }

    // ==================== getViolations: filter branching ====================

    @Test
    public void getViolations_noFilter_callsFindAll() throws Exception {
        when(violationService.findAll()).thenReturn(Arrays.asList(violation("v-1", "1_3S", "REJECTION")));

        mockMvc.perform(get("/rest/qc/violations")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value("v-1"))
                .andExpect(jsonPath("$[0].ruleCode").value("1_3S"));
    }

    @Test
    public void getViolations_unresolvedTrue_callsFindUnresolved() throws Exception {
        when(violationService.findUnresolved()).thenReturn(Arrays.asList(violation("v-1", "1_2S", "WARNING")));

        mockMvc.perform(get("/rest/qc/violations").param("unresolved", "true")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    public void getViolations_byInstrument_callsFindByInstrument() throws Exception {
        when(violationService.findByInstrument("23")).thenReturn(Collections.<QCRuleViolation>emptyList());

        mockMvc.perform(get("/rest/qc/violations").param("instrumentId", "23")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void getViolations_byInstrumentAndUnresolved_callsFindUnresolvedByInstrument() throws Exception {
        when(violationService.findUnresolvedByInstrument("23"))
                .thenReturn(Arrays.asList(violation("v-1", "2_2S", "REJECTION")));

        mockMvc.perform(get("/rest/qc/violations").param("instrumentId", "23").param("unresolved", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].ruleCode").value("2_2S"));
    }

    @Test
    public void getViolations_bySeverity_callsFindBySeverity() throws Exception {
        when(violationService.findBySeverity("REJECTION"))
                .thenReturn(Arrays.asList(violation("v-1", "1_3S", "REJECTION")));

        mockMvc.perform(get("/rest/qc/violations").param("severity", "REJECTION")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].severity").value("REJECTION"));
    }

    // ==================== getViolation (single) ====================

    @Test
    public void getViolation_returns200WhenFound() throws Exception {
        when(violationService.getById("v-1")).thenReturn(violation("v-1", "1_3S", "REJECTION"));

        mockMvc.perform(get("/rest/qc/violations/v-1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("v-1"));
    }

    @Test
    public void getViolation_returns404WhenNull() throws Exception {
        when(violationService.getById("missing")).thenReturn(null);

        mockMvc.perform(get("/rest/qc/violations/missing")).andExpect(status().isNotFound());
    }

    // ==================== getViolationCounts ====================

    @Test
    public void getViolationCounts_returnsAggregatedCounts() throws Exception {
        when(violationService.getUnresolvedCountBySeverity("REJECTION")).thenReturn(3);
        when(violationService.getUnresolvedCountBySeverity("WARNING")).thenReturn(5);

        mockMvc.perform(get("/rest/qc/violations/counts")).andExpect(status().isOk())
                .andExpect(jsonPath("$.rejectionCount").value(3)).andExpect(jsonPath("$.warningCount").value(5))
                .andExpect(jsonPath("$.totalCount").value(8));
    }

    // ==================== helpers ====================

    private QCRuleViolation violation(String id, String ruleCode, String severity) {
        QCRuleViolation v = new QCRuleViolation();
        v.setId(id);
        v.setRuleCode(ruleCode);
        v.setSeverity(severity);
        return v;
    }
}
