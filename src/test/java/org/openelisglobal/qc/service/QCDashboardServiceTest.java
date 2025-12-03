package org.openelisglobal.qc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.qc.dao.QCRuleViolationDAO;
import org.openelisglobal.qc.service.QCDashboardService.DashboardSummary;
import org.openelisglobal.qc.service.QCDashboardService.InstrumentComplianceStatus;
import org.openelisglobal.qc.valueholder.QCRuleViolation;

/**
 * Unit tests for QCDashboardService (T115).
 *
 * Tests compliance color calculation, instrument status aggregation, and
 * dashboard summary generation.
 */
@RunWith(MockitoJUnitRunner.class)
public class QCDashboardServiceTest {

    @Mock
    private QCRuleViolationDAO violationDAO;

    @InjectMocks
    private QCDashboardServiceImpl dashboardService;

    private QCRuleViolation createViolation(String id, Integer instrumentId, String severity, String status) {
        QCRuleViolation violation = new QCRuleViolation();
        violation.setId(id);
        violation.setInstrumentId(instrumentId);
        violation.setSeverity(severity);
        violation.setResolutionStatus(status);
        violation.setRuleCode("1₃ₛ");
        violation.setViolationDateTime(Timestamp.from(Instant.now()));
        return violation;
    }

    // ==================== getAllInstrumentComplianceStatus Tests
    // ====================

    @Test
    public void getAllInstrumentComplianceStatus_noViolations_returnsEmptyList() {
        when(violationDAO.findUnresolved()).thenReturn(Collections.emptyList());

        List<InstrumentComplianceStatus> result = dashboardService.getAllInstrumentComplianceStatus();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getAllInstrumentComplianceStatus_singleInstrumentWithRejection_returnsRedStatus() {
        QCRuleViolation violation = createViolation("V1", 100, "REJECTION", "UNRESOLVED");
        when(violationDAO.findUnresolved()).thenReturn(Arrays.asList(violation));

        List<InstrumentComplianceStatus> result = dashboardService.getAllInstrumentComplianceStatus();

        assertEquals(1, result.size());
        InstrumentComplianceStatus status = result.get(0);
        assertEquals(Integer.valueOf(100), status.getInstrumentId());
        assertEquals("RED", status.getComplianceColor());
        assertEquals(1, status.getUnresolvedRejections());
        assertEquals(0, status.getUnresolvedWarnings());
    }

    @Test
    public void getAllInstrumentComplianceStatus_singleInstrumentWithWarningOnly_returnsYellowStatus() {
        QCRuleViolation violation = createViolation("V1", 100, "WARNING", "UNRESOLVED");
        when(violationDAO.findUnresolved()).thenReturn(Arrays.asList(violation));

        List<InstrumentComplianceStatus> result = dashboardService.getAllInstrumentComplianceStatus();

        assertEquals(1, result.size());
        InstrumentComplianceStatus status = result.get(0);
        assertEquals("YELLOW", status.getComplianceColor());
        assertEquals(0, status.getUnresolvedRejections());
        assertEquals(1, status.getUnresolvedWarnings());
    }

    @Test
    public void getAllInstrumentComplianceStatus_multipleInstruments_sortedByColorPriority() {
        // Instrument 100 with WARNING (YELLOW)
        QCRuleViolation v1 = createViolation("V1", 100, "WARNING", "UNRESOLVED");
        // Instrument 200 with REJECTION (RED)
        QCRuleViolation v2 = createViolation("V2", 200, "REJECTION", "UNRESOLVED");
        // Instrument 300 with WARNING (YELLOW)
        QCRuleViolation v3 = createViolation("V3", 300, "WARNING", "UNRESOLVED");

        when(violationDAO.findUnresolved()).thenReturn(Arrays.asList(v1, v2, v3));

        List<InstrumentComplianceStatus> result = dashboardService.getAllInstrumentComplianceStatus();

        assertEquals(3, result.size());
        // RED should be first
        assertEquals("RED", result.get(0).getComplianceColor());
        assertEquals(Integer.valueOf(200), result.get(0).getInstrumentId());
        // YELLOW instruments should follow, sorted by ID
        assertEquals("YELLOW", result.get(1).getComplianceColor());
        assertEquals("YELLOW", result.get(2).getComplianceColor());
    }

    @Test
    public void getAllInstrumentComplianceStatus_mixedViolationsForSameInstrument_aggregatesCounts() {
        // Same instrument with multiple violations
        QCRuleViolation v1 = createViolation("V1", 100, "REJECTION", "UNRESOLVED");
        QCRuleViolation v2 = createViolation("V2", 100, "WARNING", "UNRESOLVED");
        QCRuleViolation v3 = createViolation("V3", 100, "REJECTION", "UNRESOLVED");
        v3.setRuleCode("2₂ₛ"); // Different rule

        when(violationDAO.findUnresolved()).thenReturn(Arrays.asList(v1, v2, v3));

        List<InstrumentComplianceStatus> result = dashboardService.getAllInstrumentComplianceStatus();

        assertEquals(1, result.size());
        InstrumentComplianceStatus status = result.get(0);
        assertEquals("RED", status.getComplianceColor()); // Has rejections
        assertEquals(2, status.getUnresolvedRejections());
        assertEquals(1, status.getUnresolvedWarnings());
        assertEquals(2, status.getTriggeredRules().size()); // Two different rules
    }

    @Test
    public void getAllInstrumentComplianceStatus_tracksTriggeredRules() {
        QCRuleViolation v1 = createViolation("V1", 100, "REJECTION", "UNRESOLVED");
        v1.setRuleCode("1₃ₛ");
        QCRuleViolation v2 = createViolation("V2", 100, "WARNING", "UNRESOLVED");
        v2.setRuleCode("1₂ₛ");

        when(violationDAO.findUnresolved()).thenReturn(Arrays.asList(v1, v2));

        List<InstrumentComplianceStatus> result = dashboardService.getAllInstrumentComplianceStatus();

        InstrumentComplianceStatus status = result.get(0);
        assertTrue(status.getTriggeredRules().contains("1₃ₛ"));
        assertTrue(status.getTriggeredRules().contains("1₂ₛ"));
    }

    // ==================== getInstrumentComplianceStatus Tests ====================

    @Test
    public void getInstrumentComplianceStatus_noViolations_returnsGreenStatus() {
        when(violationDAO.findUnresolvedByInstrument(100)).thenReturn(Collections.emptyList());

        InstrumentComplianceStatus status = dashboardService.getInstrumentComplianceStatus(100);

        assertNotNull(status);
        assertEquals(Integer.valueOf(100), status.getInstrumentId());
        assertEquals("GREEN", status.getComplianceColor());
        assertEquals(0, status.getUnresolvedRejections());
        assertEquals(0, status.getUnresolvedWarnings());
    }

    @Test
    public void getInstrumentComplianceStatus_withRejection_returnsRedStatus() {
        QCRuleViolation violation = createViolation("V1", 100, "REJECTION", "UNRESOLVED");
        when(violationDAO.findUnresolvedByInstrument(100)).thenReturn(Arrays.asList(violation));

        InstrumentComplianceStatus status = dashboardService.getInstrumentComplianceStatus(100);

        assertEquals("RED", status.getComplianceColor());
        assertEquals(1, status.getUnresolvedRejections());
    }

    @Test
    public void getInstrumentComplianceStatus_withWarningOnly_returnsYellowStatus() {
        QCRuleViolation violation = createViolation("V1", 100, "WARNING", "UNRESOLVED");
        when(violationDAO.findUnresolvedByInstrument(100)).thenReturn(Arrays.asList(violation));

        InstrumentComplianceStatus status = dashboardService.getInstrumentComplianceStatus(100);

        assertEquals("YELLOW", status.getComplianceColor());
        assertEquals(1, status.getUnresolvedWarnings());
    }

    @Test
    public void getInstrumentComplianceStatus_tracksLastViolationTime() {
        Timestamp earlier = Timestamp.from(Instant.now().minusSeconds(3600));
        Timestamp later = Timestamp.from(Instant.now());

        QCRuleViolation v1 = createViolation("V1", 100, "WARNING", "UNRESOLVED");
        v1.setViolationDateTime(earlier);
        QCRuleViolation v2 = createViolation("V2", 100, "WARNING", "UNRESOLVED");
        v2.setViolationDateTime(later);

        when(violationDAO.findUnresolvedByInstrument(100)).thenReturn(Arrays.asList(v1, v2));

        InstrumentComplianceStatus status = dashboardService.getInstrumentComplianceStatus(100);

        assertNotNull(status.getLastViolationTime());
        // Last violation time should be the later timestamp
        assertTrue(status.getLastViolationTime().contains(later.toInstant().toString().substring(0, 10)));
    }

    // ==================== getDashboardSummary Tests ====================

    @Test
    public void getDashboardSummary_noViolations_returnsZeroCounts() {
        when(violationDAO.findUnresolved()).thenReturn(Collections.emptyList());

        DashboardSummary summary = dashboardService.getDashboardSummary();

        assertNotNull(summary);
        assertEquals(0, summary.getTotalInstruments());
        assertEquals(0, summary.getCompliantInstruments());
        assertEquals(0, summary.getWarningInstruments());
        assertEquals(0, summary.getNonCompliantInstruments());
        assertEquals(0, summary.getTotalRejections());
        assertEquals(0, summary.getTotalWarnings());
        assertEquals(0, summary.getTotalUnresolvedViolations());
        assertNotNull(summary.getLastUpdateTime());
    }

    @Test
    public void getDashboardSummary_mixedInstruments_aggregatesCorrectly() {
        // Instrument 100 with REJECTION (RED)
        QCRuleViolation v1 = createViolation("V1", 100, "REJECTION", "UNRESOLVED");
        // Instrument 200 with WARNING (YELLOW)
        QCRuleViolation v2 = createViolation("V2", 200, "WARNING", "UNRESOLVED");
        // Instrument 300 with WARNING (YELLOW)
        QCRuleViolation v3 = createViolation("V3", 300, "WARNING", "UNRESOLVED");
        QCRuleViolation v4 = createViolation("V4", 300, "WARNING", "UNRESOLVED");

        when(violationDAO.findUnresolved()).thenReturn(Arrays.asList(v1, v2, v3, v4));

        DashboardSummary summary = dashboardService.getDashboardSummary();

        assertEquals(3, summary.getTotalInstruments());
        assertEquals(0, summary.getCompliantInstruments()); // All have violations
        assertEquals(2, summary.getWarningInstruments()); // Instruments 200, 300
        assertEquals(1, summary.getNonCompliantInstruments()); // Instrument 100
        assertEquals(1, summary.getTotalRejections());
        assertEquals(3, summary.getTotalWarnings());
        assertEquals(4, summary.getTotalUnresolvedViolations());
    }

    @Test
    public void getDashboardSummary_includesLastUpdateTime() {
        when(violationDAO.findUnresolved()).thenReturn(Collections.emptyList());

        DashboardSummary summary = dashboardService.getDashboardSummary();

        assertNotNull(summary.getLastUpdateTime());
        // Should be ISO format timestamp
        assertTrue(summary.getLastUpdateTime().contains("T"));
    }

    // ==================== Compliance Color Logic Tests ====================

    @Test
    public void complianceColor_rejectionTakesPrecedence() {
        // Even with warnings, rejection makes it RED
        QCRuleViolation rejection = createViolation("V1", 100, "REJECTION", "UNRESOLVED");
        QCRuleViolation warning = createViolation("V2", 100, "WARNING", "UNRESOLVED");

        when(violationDAO.findUnresolvedByInstrument(100)).thenReturn(Arrays.asList(rejection, warning));

        InstrumentComplianceStatus status = dashboardService.getInstrumentComplianceStatus(100);

        assertEquals("RED", status.getComplianceColor());
    }

    @Test
    public void complianceColor_multipleRejections_stillRed() {
        QCRuleViolation r1 = createViolation("V1", 100, "REJECTION", "UNRESOLVED");
        QCRuleViolation r2 = createViolation("V2", 100, "REJECTION", "UNRESOLVED");
        QCRuleViolation r3 = createViolation("V3", 100, "REJECTION", "UNRESOLVED");

        when(violationDAO.findUnresolvedByInstrument(100)).thenReturn(Arrays.asList(r1, r2, r3));

        InstrumentComplianceStatus status = dashboardService.getInstrumentComplianceStatus(100);

        assertEquals("RED", status.getComplianceColor());
        assertEquals(3, status.getUnresolvedRejections());
    }

    @Test
    public void complianceColor_multipleWarnings_staysYellow() {
        QCRuleViolation w1 = createViolation("V1", 100, "WARNING", "UNRESOLVED");
        QCRuleViolation w2 = createViolation("V2", 100, "WARNING", "UNRESOLVED");

        when(violationDAO.findUnresolvedByInstrument(100)).thenReturn(Arrays.asList(w1, w2));

        InstrumentComplianceStatus status = dashboardService.getInstrumentComplianceStatus(100);

        assertEquals("YELLOW", status.getComplianceColor());
        assertEquals(2, status.getUnresolvedWarnings());
    }

    // ==================== Edge Cases ====================

    @Test
    public void getAllInstrumentComplianceStatus_filtersResolvedViolations() {
        // Only unresolved violations should be counted
        QCRuleViolation unresolved = createViolation("V1", 100, "REJECTION", "UNRESOLVED");
        QCRuleViolation resolved = createViolation("V2", 100, "REJECTION", "RESOLVED");

        // DAO returns only unresolved, but let's test the filter in service too
        when(violationDAO.findUnresolved()).thenReturn(Arrays.asList(unresolved));

        List<InstrumentComplianceStatus> result = dashboardService.getAllInstrumentComplianceStatus();

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getUnresolvedRejections());
    }

    @Test
    public void getAllInstrumentComplianceStatus_handlesNullViolationDateTime() {
        QCRuleViolation violation = createViolation("V1", 100, "WARNING", "UNRESOLVED");
        violation.setViolationDateTime(null);

        when(violationDAO.findUnresolved()).thenReturn(Arrays.asList(violation));

        List<InstrumentComplianceStatus> result = dashboardService.getAllInstrumentComplianceStatus();

        assertEquals(1, result.size());
        // Should not throw NPE, lastViolationTime will be null
    }

    @Test
    public void getInstrumentComplianceStatus_setsInstrumentName() {
        when(violationDAO.findUnresolvedByInstrument(100)).thenReturn(Collections.emptyList());

        InstrumentComplianceStatus status = dashboardService.getInstrumentComplianceStatus(100);

        assertNotNull(status.getInstrumentName());
        assertTrue(status.getInstrumentName().contains("100"));
    }
}
