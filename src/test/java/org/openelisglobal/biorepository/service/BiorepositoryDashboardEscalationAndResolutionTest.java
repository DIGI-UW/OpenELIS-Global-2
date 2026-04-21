package org.openelisglobal.biorepository.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BiorepositoryDashboardEscalationAndResolutionTest {

    @Mock
    private BioSampleService bioSampleService;
    @Mock
    private BiorepositoryQCInspectionService qcInspectionService;
    @Mock
    private SampleRetrievalService retrievalService;
    @Mock
    private org.openelisglobal.notebook.service.NotebookEntryTemperatureLogService temperatureLogService;
    @Mock
    private org.openelisglobal.notebook.service.NotebookEntryRoomEnvironmentLogService roomEnvironmentLogService;
    @Mock
    private SampleStorageService storageService;

    @InjectMocks
    private BiorepositoryDashboardServiceImpl dashboardService;

    @Test
    public void qcMetrics_ExposeEscalationAndResolutionSummaries() {
        List<BiorepositoryQCInspection> inspections = new ArrayList<>();
        inspections.add(buildInspection(1, BiorepositoryQCInspection.QCResult.VERIFIED, null, null,
                "Freezer-A > Shelf-1 > Rack-1 > Box-1", "A1"));
        inspections.add(buildInspection(2, BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND,
                BiorepositoryQCInspection.DiscrepancyType.MISPLACED_SAMPLE_FOUND, null,
                "Freezer-A > Shelf-1 > Rack-1 > Box-1", "A2"));
        inspections.add(buildInspection(3, BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND,
                BiorepositoryQCInspection.DiscrepancyType.MISPLACED_SAMPLE_FOUND, "UPDATE_LOCATION",
                "Freezer-A > Shelf-1 > Rack-1 > Box-1", "A3"));
        inspections.add(buildInspection(4, BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND,
                BiorepositoryQCInspection.DiscrepancyType.SAMPLE_MISSING, "MARK_MISSING",
                "Freezer-A > Shelf-1 > Rack-2 > Box-4", "B4"));

        when(qcInspectionService.getAll()).thenReturn(inspections);

        Map<String, Object> metrics = dashboardService.getQCComplianceMetrics();
        @SuppressWarnings("unchecked")
        Map<String, Object> resolution = (Map<String, Object>) metrics.get("failureResolutionSummary");
        @SuppressWarnings("unchecked")
        Map<String, Object> correctedVsUnresolved = (Map<String, Object>) resolution.get("correctedVsUnresolved");
        @SuppressWarnings("unchecked")
        Map<String, Object> escalationSignals = (Map<String, Object>) metrics.get("escalationSignals");

        assertNotNull(resolution);
        assertEquals(1L, ((Number) resolution.get("failedPendingCorrection")).longValue());
        assertEquals(1L, ((Number) resolution.get("failedCorrected")).longValue());
        assertEquals(1L, ((Number) resolution.get("failedMarkedMissing")).longValue());
        assertEquals(2L, ((Number) correctedVsUnresolved.get("corrected")).longValue());
        assertEquals(1L, ((Number) correctedVsUnresolved.get("unresolved")).longValue());

        assertNotNull(escalationSignals);
        assertTrue(Boolean.TRUE.equals(escalationSignals.get("batchFailRateExceeded")));
        assertTrue(Boolean.TRUE.equals(escalationSignals.get("repeatedFailureInSameBoxOrRack")));
        assertTrue(Boolean.TRUE.equals(escalationSignals.get("criticalSamplesMissing")));
    }

    @Test
    public void qcHistory_ExposesPerRecordFlagsAndFailureResolutionStatus() {
        List<BiorepositoryQCInspection> inspections = new ArrayList<>();
        inspections.add(buildInspection(10, BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND,
                BiorepositoryQCInspection.DiscrepancyType.MISPLACED_SAMPLE_FOUND, null,
                "Freezer-B > Shelf-2 > Rack-3 > Box-8", "A1"));
        inspections.add(buildInspection(11, BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND,
                BiorepositoryQCInspection.DiscrepancyType.MISPLACED_SAMPLE_FOUND, "UPDATE_LOCATION",
                "Freezer-B > Shelf-2 > Rack-3 > Box-8", "A2"));

        when(qcInspectionService.getAll()).thenReturn(inspections);

        Map<String, Object> history = dashboardService.getQCHistory(10);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) history.get("items");
        assertEquals(2, items.size());

        Map<String, Object> pending = items.stream()
                .filter(item -> "FAILED_PENDING_CORRECTION".equals(item.get("lifecycleOutcome"))).findFirst()
                .orElseThrow(() -> new AssertionError("Missing pending correction item"));
        Map<String, Object> corrected = items.stream()
                .filter(item -> "FAILED_CORRECTED".equals(item.get("lifecycleOutcome"))).findFirst()
                .orElseThrow(() -> new AssertionError("Missing corrected item"));

        assertEquals("UNDER_INVESTIGATION", pending.get("boxFlag"));
        assertEquals("UNDER_INVESTIGATION", corrected.get("boxFlag"));
        assertEquals("UNRESOLVED", pending.get("failureResolution"));
        assertEquals("RESOLVED", corrected.get("failureResolution"));
        assertTrue(Boolean.TRUE.equals(pending.get("escalationTriggered")));
        assertTrue(Boolean.TRUE.equals(corrected.get("escalationTriggered")));

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) history.get("failureResolutionSummary");
        assertNotNull(summary);
        assertEquals(1L, ((Number) summary.get("failedPendingCorrection")).longValue());
        assertEquals(1L, ((Number) summary.get("failedCorrected")).longValue());
        assertFalse(items.isEmpty());
    }

    @Test
    public void qcMetrics_BatchFailRateExactlyFivePercent_DoesNotTriggerOverFiveRule() {
        List<BiorepositoryQCInspection> inspections = new ArrayList<>();
        // 20 total, 1 failed => exactly 5.0%
        for (int i = 1; i <= 19; i++) {
            inspections.add(buildInspection(100 + i, BiorepositoryQCInspection.QCResult.VERIFIED, null, null,
                    "Freezer-C > Shelf-1 > Rack-1 > Box-1", "A" + i));
        }
        inspections.add(buildInspection(200, BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND,
                BiorepositoryQCInspection.DiscrepancyType.MISPLACED_SAMPLE_FOUND, null,
                "Freezer-C > Shelf-1 > Rack-1 > Box-1", "Z1"));

        when(qcInspectionService.getAll()).thenReturn(inspections);

        Map<String, Object> metrics = dashboardService.getQCComplianceMetrics();
        @SuppressWarnings("unchecked")
        Map<String, Object> escalationSignals = (Map<String, Object>) metrics.get("escalationSignals");

        assertNotNull(escalationSignals);
        assertEquals(5.0, ((Number) escalationSignals.get("batchFailRatePercent")).doubleValue(), 0.0001);
        assertFalse(Boolean.TRUE.equals(escalationSignals.get("batchFailRateExceeded")));

        @SuppressWarnings("unchecked")
        List<String> triggeredRules = (List<String>) escalationSignals.get("triggeredRules");
        assertFalse(triggeredRules.contains("BATCH_FAIL_RATE_OVER_5_PERCENT"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flaggedFreezers = (List<Map<String, Object>>) escalationSignals.get("flaggedFreezers");
        assertTrue("Freezer should not be flagged at exactly 5.0%", flaggedFreezers.isEmpty());
    }

    @Test
    public void qcMetrics_BatchFailRateAboveFivePercent_TriggersOverFiveRuleAndFlagsFreezer() {
        List<BiorepositoryQCInspection> inspections = new ArrayList<>();
        // 20 total, 2 failed => 10.0%
        for (int i = 1; i <= 18; i++) {
            inspections.add(buildInspection(300 + i, BiorepositoryQCInspection.QCResult.VERIFIED, null, null,
                    "Freezer-D > Shelf-1 > Rack-1 > Box-1", "A" + i));
        }
        inspections.add(buildInspection(400, BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND,
                BiorepositoryQCInspection.DiscrepancyType.MISPLACED_SAMPLE_FOUND, null,
                "Freezer-D > Shelf-1 > Rack-1 > Box-1", "Z1"));
        inspections.add(buildInspection(401, BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND,
                BiorepositoryQCInspection.DiscrepancyType.SAMPLE_MISSING, "MARK_MISSING",
                "Freezer-D > Shelf-1 > Rack-1 > Box-1", "Z2"));

        when(qcInspectionService.getAll()).thenReturn(inspections);

        Map<String, Object> metrics = dashboardService.getQCComplianceMetrics();
        @SuppressWarnings("unchecked")
        Map<String, Object> escalationSignals = (Map<String, Object>) metrics.get("escalationSignals");

        assertNotNull(escalationSignals);
        assertTrue(((Number) escalationSignals.get("batchFailRatePercent")).doubleValue() > 5.0);
        assertTrue(Boolean.TRUE.equals(escalationSignals.get("batchFailRateExceeded")));

        @SuppressWarnings("unchecked")
        List<String> triggeredRules = (List<String>) escalationSignals.get("triggeredRules");
        assertTrue(triggeredRules.contains("BATCH_FAIL_RATE_OVER_5_PERCENT"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flaggedFreezers = (List<Map<String, Object>>) escalationSignals.get("flaggedFreezers");
        assertFalse("Freezer should be flagged above 5.0% fail rate", flaggedFreezers.isEmpty());
        assertEquals("Freezer-D", flaggedFreezers.get(0).get("key"));
    }

    private BiorepositoryQCInspection buildInspection(int id, BiorepositoryQCInspection.QCResult qcResult,
            BiorepositoryQCInspection.DiscrepancyType discrepancyType, String correctionActionType, String locationPath,
            String position) {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId(String.valueOf(1000 + id));

        BioSample bioSample = new BioSample();
        bioSample.setId(id);
        bioSample.setSampleItem(sampleItem);

        BiorepositoryQCInspection inspection = new BiorepositoryQCInspection();
        inspection.setId(id);
        inspection.setBioSample(bioSample);
        inspection.setInspectorName("Inspector-" + id);
        inspection.setSysUserId("7");
        inspection.setInspectionDate(new Timestamp(System.currentTimeMillis() - id * 1000L));
        inspection.setQcResult(qcResult);
        inspection.setDiscrepancyType(discrepancyType);
        inspection.setCorrectiveAction("QC note " + id);
        inspection.setRemarks("QC remark " + id);
        inspection.setExpectedLocationPath(locationPath);
        inspection.setExpectedPositionCoordinate(position);
        inspection.setSamplePresent(!BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.equals(qcResult));
        inspection.setLabelIntegrity(true);
        inspection.setContainerIntegrity(true);
        inspection.setVolumeAppearanceAcceptable(true);
        inspection.setCorrectPosition(!BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.equals(qcResult));
        inspection.setCorrectionActionType(correctionActionType);
        if (correctionActionType != null) {
            inspection.setCorrectionOldCoordinate(locationPath + " > " + position);
            inspection.setCorrectionNewCoordinate("MARK_MISSING".equals(correctionActionType)
                    ? "Missing (not found during QC)"
                    : locationPath + " > Z9");
            inspection.setCorrectionReason(correctionActionType + ": corrected");
            inspection.setCorrectionByUser("7");
            inspection.setCorrectionTimestamp(new Timestamp(System.currentTimeMillis()));
        }
        return inspection;
    }
}
