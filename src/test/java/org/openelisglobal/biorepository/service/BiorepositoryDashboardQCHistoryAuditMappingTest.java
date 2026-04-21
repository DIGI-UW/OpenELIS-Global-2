package org.openelisglobal.biorepository.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
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
public class BiorepositoryDashboardQCHistoryAuditMappingTest {

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
    public void qcHistory_UsesPersistedCorrectionCoordinatesWhenCorrectionApplied() {
        BiorepositoryQCInspection inspection = buildInspection("UPDATE_LOCATION", "Freezer-A > Shelf-1 > Rack-2 > Box-3 > B7");

        // Simulate current location drift after correction; history should still show persisted correction coordinate.
        when(storageService.getSampleItemLocation(anyString()))
                .thenReturn(Map.of("hierarchicalPath", "Freezer-Z > Shelf-9 > Rack-9 > Box-9", "positionCoordinate", "Z1"));
        when(qcInspectionService.getAll()).thenReturn(new ArrayList<>(List.of(inspection)));

        Map<String, Object> history = dashboardService.getQCHistory(10);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) history.get("items");

        assertEquals(1, items.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> auditTrail = (Map<String, Object>) items.get(0).get("auditTrail");
        assertNotNull(auditTrail);
        assertEquals("Freezer-A > Shelf-1 > Rack-2 > Box-3 > B7", auditTrail.get("newCoordinate"));
    }

    @Test
    public void qcHistory_UsesCurrentLocationFallbackWhenNoCorrectionApplied() {
        BiorepositoryQCInspection inspection = buildInspection(null, null);

        when(storageService.getSampleItemLocation(anyString()))
                .thenReturn(Map.of("hierarchicalPath", "Freezer-B > Shelf-2 > Rack-1 > Box-4", "positionCoordinate", "C2"));
        when(qcInspectionService.getAll()).thenReturn(new ArrayList<>(List.of(inspection)));

        Map<String, Object> history = dashboardService.getQCHistory(10);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) history.get("items");

        @SuppressWarnings("unchecked")
        Map<String, Object> auditTrail = (Map<String, Object>) items.get(0).get("auditTrail");
        assertEquals("Freezer-B > Shelf-2 > Rack-1 > Box-4 > C2", auditTrail.get("newCoordinate"));
    }

    private BiorepositoryQCInspection buildInspection(String correctionActionType, String correctionNewCoordinate) {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("101");

        BioSample bioSample = new BioSample();
        bioSample.setId(77);
        bioSample.setSampleItem(sampleItem);

        BiorepositoryQCInspection inspection = new BiorepositoryQCInspection();
        inspection.setId(11);
        inspection.setBioSample(bioSample);
        inspection.setInspectorName("Inspector");
        inspection.setSysUserId("7");
        inspection.setInspectionDate(new Timestamp(System.currentTimeMillis()));
        inspection.setQcResult(BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND);
        inspection.setDiscrepancyType(BiorepositoryQCInspection.DiscrepancyType.MISPLACED_SAMPLE_FOUND);
        inspection.setCorrectiveAction("Discrepancy note");
        inspection.setExpectedLocationPath("Freezer-A > Shelf-1 > Rack-2 > Box-3");
        inspection.setExpectedPositionCoordinate("A1");
        inspection.setCorrectionActionType(correctionActionType);
        inspection.setCorrectionOldCoordinate("Freezer-A > Shelf-1 > Rack-2 > Box-3 > A1");
        inspection.setCorrectionNewCoordinate(correctionNewCoordinate);
        inspection.setCorrectionReason(correctionActionType != null ? correctionActionType + ": corrected" : null);
        inspection.setCorrectionByUser(correctionActionType != null ? "7" : null);
        inspection.setCorrectionTimestamp(correctionActionType != null ? new Timestamp(System.currentTimeMillis()) : null);
        return inspection;
    }
}
