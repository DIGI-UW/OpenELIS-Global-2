package org.openelisglobal.biorepository.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.BiorepositoryQCInspectionService;
import org.openelisglobal.biorepository.service.BiorepositoryQcSamplePoolService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BiorepositoryQCInspectionRestControllerFlowTest {

    @Mock
    private BiorepositoryQCInspectionService qcInspectionService;
    @Mock
    private BioSampleService bioSampleService;
    @Mock
    private SampleStorageService storageService;
    @Mock
    private StorageLocationService storageLocationService;
    @Mock
    private BiorepositoryQcSamplePoolService qcSamplePoolService;

    @InjectMocks
    private BiorepositoryQCInspectionRestController controller;

    private HttpServletRequest requestWithUser;

    @Before
    public void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(7);
        request.getSession().setAttribute("userSessionData", usd);
        requestWithUser = request;
    }

    @Test
    public void bulkApply_UpdateLocation_ReturnsCorrectedOutcomeAndAuditFields() {
        runCorrectionFlowAssertion("UPDATE_LOCATION", BiorepositoryQCInspection.DiscrepancyType.MISPLACED_SAMPLE_FOUND,
                "FAILED_CORRECTED", "QC_FAILED");
    }

    @Test
    public void bulkApply_ReassignPosition_ReturnsCorrectedOutcomeAndAuditFields() {
        runCorrectionFlowAssertion("REASSIGN_POSITION", BiorepositoryQCInspection.DiscrepancyType.MISPLACED_SAMPLE_FOUND,
                "FAILED_CORRECTED", "QC_FAILED");
    }

    @Test
    public void bulkApply_MarkMissing_ReturnsMissingOutcomeAndAuditFields() {
        runCorrectionFlowAssertion("MARK_MISSING", BiorepositoryQCInspection.DiscrepancyType.SAMPLE_MISSING,
                "FAILED_MARKED_MISSING", "MISSING");
    }

    @Test
    public void bulkApply_DiscrepancyWithoutCorrection_UsesCurrentLocationAsAuditFallback() {
        BiorepositoryQCInspection inspection = buildInspection(
                BiorepositoryQCInspection.DiscrepancyType.MISPLACED_SAMPLE_FOUND, null, "QC_FAILED");
        BiorepositoryQCInspectionRestController.BulkQCInspectionRequest request = buildRequest(null,
                BiorepositoryQCInspection.DiscrepancyType.MISPLACED_SAMPLE_FOUND.name());

        when(qcInspectionService.createBulkInspections(any(), any(), any(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(), anyBoolean(), any(), any(), any(), any(), any(), eq("7")))
                        .thenReturn(List.of(inspection));
        when(storageService.getSampleItemLocation("101")).thenReturn(Map.of("hierarchicalPath",
                "Freezer-A > Shelf-1 > Rack-1 > Box-9", "positionCoordinate", "D2"));

        ResponseEntity<?> response = controller.bulkApplyQC(request, requestWithUser);
        assertEquals(200, response.getStatusCode().value());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inspections = (List<Map<String, Object>>) body.get("inspections");
        Map<String, Object> mapped = inspections.get(0);

        assertEquals("FAILED_PENDING_CORRECTION", mapped.get("lifecycleOutcome"));
        @SuppressWarnings("unchecked")
        Map<String, Object> auditTrail = (Map<String, Object>) mapped.get("auditTrail");
        assertEquals("Freezer-A > Shelf-1 > Rack-1 > Box-9 > D2", auditTrail.get("newCoordinate"));
        verify(storageService).getSampleItemLocation("101");
    }

    private void runCorrectionFlowAssertion(String correctionActionType,
            BiorepositoryQCInspection.DiscrepancyType discrepancyType, String expectedLifecycle, String expectedStatus) {
        BiorepositoryQCInspection inspection = buildInspection(discrepancyType, correctionActionType, expectedStatus);
        BiorepositoryQCInspectionRestController.BulkQCInspectionRequest request = buildRequest(correctionActionType,
                discrepancyType.name());

        when(qcInspectionService.createBulkInspections(any(), any(), any(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(), anyBoolean(), any(), any(), any(), any(), any(), eq("7")))
                        .thenReturn(List.of(inspection));
        if ("MARK_MISSING".equals(correctionActionType)) {
            java.util.HashMap<String, Object> missingLocation = new java.util.HashMap<>();
            missingLocation.put("hierarchicalPath", "Missing (not found during QC)");
            missingLocation.put("positionCoordinate", null);
            missingLocation.put("status", "MISSING");

            java.util.HashMap<String, Object> missingResult = new java.util.HashMap<>();
            missingResult.put("movementId", "mv-1");
            missingResult.put("updatedLocation", missingLocation);
            when(storageService.markSampleItemMissing(eq("101"), any(), any())).thenReturn(missingResult);
        } else {
            when(storageService.moveSampleItemWithLocation(eq("101"), eq("301"), eq("rack"), eq("B5"), any(), any()))
                    .thenReturn("mv-1");
            when(storageService.getSampleItemLocation("101")).thenReturn(Map.of(
                    "hierarchicalPath", "Freezer-A > Shelf-1 > Rack-1 > Box-2",
                    "positionCoordinate", "B5"));
        }

        ResponseEntity<?> response = controller.bulkApplyQC(request, requestWithUser);
        assertEquals(200, response.getStatusCode().value());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inspections = (List<Map<String, Object>>) body.get("inspections");
        assertEquals(1, inspections.size());
        Map<String, Object> mapped = inspections.get(0);

        assertEquals(expectedLifecycle, mapped.get("lifecycleOutcome"));
        assertEquals(expectedStatus, mapped.get("qcStatus"));
        assertEquals(correctionActionType, mapped.get("correctionActionType"));
        assertEquals("7", mapped.get("correctionByUser"));
        assertNotNull(mapped.get("correctionTimestamp"));

        @SuppressWarnings("unchecked")
        Map<String, Object> auditTrail = (Map<String, Object>) mapped.get("auditTrail");
        assertNotNull(auditTrail);
        assertNotNull(auditTrail.get("oldCoordinate"));
        assertNotNull(auditTrail.get("newCoordinate"));
        assertEquals("7", auditTrail.get("correctedBy"));
        assertTrue(String.valueOf(auditTrail.get("reason")).startsWith(correctionActionType + ":"));
    }

    @Test
    public void bulkApply_MarkMissingWithNonMissingDiscrepancy_ReturnsBadRequest() {
        BiorepositoryQCInspectionRestController.BulkQCInspectionRequest request = buildRequest("MARK_MISSING",
                BiorepositoryQCInspection.DiscrepancyType.MISPLACED_SAMPLE_FOUND.name());

        ResponseEntity<?> response = controller.bulkApplyQC(request, requestWithUser);
        assertEquals(400, response.getStatusCode().value());

        @SuppressWarnings("unchecked")
        Map<String, Object> errorBody = (Map<String, Object>) response.getBody();
        assertNotNull(errorBody);
        assertTrue(String.valueOf(errorBody.get("error")).contains("MARK_MISSING requires discrepancy type SAMPLE_MISSING"));
        verifyZeroInteractions(qcInspectionService);
    }

    @Test
    public void bulkApply_MarkMissingWithLocationFields_ReturnsBadRequest() {
        BiorepositoryQCInspectionRestController.BulkQCInspectionRequest request = buildRequest("MARK_MISSING",
                BiorepositoryQCInspection.DiscrepancyType.SAMPLE_MISSING.name());
        request.setCorrectionLocationType("box");
        request.setCorrectionLocationId("301");
        request.setCorrectionPositionCoordinate("B5");

        ResponseEntity<?> response = controller.bulkApplyQC(request, requestWithUser);
        assertEquals(400, response.getStatusCode().value());

        @SuppressWarnings("unchecked")
        Map<String, Object> errorBody = (Map<String, Object>) response.getBody();
        assertNotNull(errorBody);
        assertTrue(String.valueOf(errorBody.get("error"))
                .contains("MARK_MISSING does not allow correction location/position fields"));
        verifyZeroInteractions(qcInspectionService);
    }

    @Test
    public void bulkApply_UpdateLocationWithoutLocationFields_ReturnsBadRequest() {
        BiorepositoryQCInspectionRestController.BulkQCInspectionRequest request = buildRequest("UPDATE_LOCATION",
                BiorepositoryQCInspection.DiscrepancyType.MISPLACED_SAMPLE_FOUND.name());
        request.setCorrectionLocationId(null);
        request.setCorrectionLocationType(null);

        ResponseEntity<?> response = controller.bulkApplyQC(request, requestWithUser);
        assertEquals(400, response.getStatusCode().value());

        @SuppressWarnings("unchecked")
        Map<String, Object> errorBody = (Map<String, Object>) response.getBody();
        assertNotNull(errorBody);
        assertTrue(String.valueOf(errorBody.get("error"))
                .contains("UPDATE_LOCATION/REASSIGN_POSITION require correctionLocationId and correctionLocationType"));
        verifyZeroInteractions(qcInspectionService);
    }

    @Test
    public void bulkApply_ReassignPositionWithoutPositionCoordinate_ReturnsBadRequest() {
        BiorepositoryQCInspectionRestController.BulkQCInspectionRequest request = buildRequest("REASSIGN_POSITION",
                BiorepositoryQCInspection.DiscrepancyType.MISPLACED_SAMPLE_FOUND.name());
        request.setCorrectionPositionCoordinate(null);

        ResponseEntity<?> response = controller.bulkApplyQC(request, requestWithUser);
        assertEquals(400, response.getStatusCode().value());

        @SuppressWarnings("unchecked")
        Map<String, Object> errorBody = (Map<String, Object>) response.getBody();
        assertNotNull(errorBody);
        assertTrue(String.valueOf(errorBody.get("error")).contains("REASSIGN_POSITION requires correctionPositionCoordinate"));
        verifyZeroInteractions(qcInspectionService);
    }

    @Test
    public void getQCStorageOverview_ExcludesAlreadyInspectedSamplesByDefault() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("eligibleSamples", List.of(Map.of("bioSampleId", 100)));
        overview.put("counts", Map.of("eligibleSamples", 1));
        stubStorageOverviewFromPool(false, null, overview);

        ResponseEntity<Map<String, Object>> response = controller.getQCStorageOverview(null, null, null, null, false,
                null);
        assertEquals(200, response.getStatusCode().value());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> eligibleSamples = (List<Map<String, Object>>) body.get("eligibleSamples");
        assertEquals(1, eligibleSamples.size());
        assertEquals(100, eligibleSamples.get(0).get("bioSampleId"));

        @SuppressWarnings("unchecked")
        Map<String, Object> counts = (Map<String, Object>) body.get("counts");
        assertEquals(1, counts.get("eligibleSamples"));
    }

    @Test
    public void getQCStorageOverview_IncludeInspectedTrue_ReturnsCompletedSamplesInPool() {
        Map<String, Object> inspectedRow = new HashMap<>();
        inspectedRow.put("bioSampleId", 101);
        inspectedRow.put("hasInspectionHistory", true);
        Map<String, Object> overview = new HashMap<>();
        overview.put("eligibleSamples", List.of(Map.of("bioSampleId", 100), inspectedRow));
        stubStorageOverviewFromPool(true, null, overview);

        ResponseEntity<Map<String, Object>> response = controller.getQCStorageOverview(null, null, null, null, true,
                null);
        assertEquals(200, response.getStatusCode().value());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> eligibleSamples = (List<Map<String, Object>>) body.get("eligibleSamples");
        assertEquals(2, eligibleSamples.size());
        assertTrue(eligibleSamples.stream().anyMatch(sample -> Integer.valueOf(101).equals(sample.get("bioSampleId"))
                && Boolean.TRUE.equals(sample.get("hasInspectionHistory"))));
    }

    @Test
    public void getQCStorageOverview_IncludesRoomPrefixedRackAssignments() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("eligibleSamples", List.of(Map.of("bioSampleId", 100, "freezer", "Freezer-A", "shelf", "Shelf-1",
                "rack", "Rack-1", "box", "Unknown")));
        stubStorageOverviewFromPool(true, null, overview);

        ResponseEntity<Map<String, Object>> response = controller.getQCStorageOverview(null, null, null, null, true,
                null);
        assertEquals(200, response.getStatusCode().value());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> eligibleSamples = (List<Map<String, Object>>) body.get("eligibleSamples");
        assertEquals(1, eligibleSamples.size());
        assertEquals("Freezer-A", eligibleSamples.get(0).get("freezer"));
        assertEquals("Shelf-1", eligibleSamples.get(0).get("shelf"));
        assertEquals("Rack-1", eligibleSamples.get(0).get("rack"));
        assertEquals("Unknown", eligibleSamples.get(0).get("box"));
    }

    private BiorepositoryQCInspectionRestController.BulkQCInspectionRequest buildRequest(String correctionActionType,
            String discrepancyType) {
        BiorepositoryQCInspectionRestController.BulkQCInspectionRequest request = new BiorepositoryQCInspectionRestController.BulkQCInspectionRequest();
        request.setBioSampleIds(List.of(99));
        request.setInspectorName("Operator");
        request.setInspectionDate(new Timestamp(System.currentTimeMillis()));
        request.setSamplePresent(false);
        request.setLabelIntegrity(true);
        request.setContainerIntegrity(true);
        request.setVolumeAppearanceAcceptable(true);
        request.setCorrectPosition(false);
        request.setDiscrepancyType(discrepancyType);
        request.setCorrectiveAction("Operator identified discrepancy");
        request.setRemarks("Operator discrepancy remarks");
        request.setCorrectionActionType(correctionActionType);
        if (correctionActionType != null) {
            request.setCorrectionLocationType("rack");
            request.setCorrectionLocationId("301");
            request.setCorrectionPositionCoordinate("B5");
            request.setCorrectionReason("operator correction");
        }
        if ("MARK_MISSING".equals(correctionActionType) || correctionActionType == null) {
            request.setCorrectionLocationType(null);
            request.setCorrectionLocationId(null);
            request.setCorrectionPositionCoordinate(null);
        }
        return request;
    }

    private BioSample buildStoredBioSample(Integer bioSampleId, String sampleItemId) {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId(sampleItemId);

        BioSample bioSample = new BioSample();
        bioSample.setId(bioSampleId);
        bioSample.setWorkflowStatus(BioSample.WorkflowStatus.STORED);
        bioSample.setSampleItem(sampleItem);
        return bioSample;
    }

    private void stubActiveStorageHierarchy() {
        StorageDevice device = new StorageDevice();
        device.setId(1);
        device.setName("Freezer-A");
        device.setType(StorageDevice.DeviceType.FREEZER.getValue());
        device.setActive(true);
        device.setBiorepositoryStorage(true);

        StorageShelf shelf = new StorageShelf();
        shelf.setId(11);
        shelf.setLabel("Shelf-1");
        shelf.setActive(true);
        shelf.setParentDevice(device);

        StorageRack rack = new StorageRack();
        rack.setId(21);
        rack.setLabel("Rack-1");
        rack.setActive(true);
        rack.setParentShelf(shelf);

        StorageBox box = new StorageBox();
        box.setId(31);
        box.setLabel("Box-1");
        box.setActive(true);
        box.setParentRack(rack);

        when(storageLocationService.getAllDevices()).thenReturn(List.of(device));
        when(storageLocationService.getAllShelves()).thenReturn(List.of(shelf));
        when(storageLocationService.getAllRacks()).thenReturn(List.of(rack));
        when(storageLocationService.getAllBoxes()).thenReturn(List.of(box));
    }

    private BiorepositoryQCInspection buildInspection(BiorepositoryQCInspection.DiscrepancyType discrepancyType,
            String correctionActionType, String expectedStatus) {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("101");

        BioSample bioSample = new BioSample();
        bioSample.setId(99);
        bioSample.setSampleItem(sampleItem);

        BiorepositoryQCInspection inspection = new BiorepositoryQCInspection();
        inspection.setId(501);
        inspection.setBioSample(bioSample);
        inspection.setInspectorName("Operator");
        inspection.setSysUserId("7");
        inspection.setInspectionDate(new Timestamp(System.currentTimeMillis()));
        inspection.setQcResult(BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND);
        inspection.setDiscrepancyType(discrepancyType);
        inspection.setCorrectiveAction((correctionActionType != null ? correctionActionType : "PENDING")
                + ": Operator identified discrepancy");
        inspection.setRemarks("Operator discrepancy remarks");
        inspection.setExpectedLocationPath("Freezer-A > Shelf-1 > Rack-1 > Box-2");
        inspection.setExpectedPositionCoordinate("A1");
        inspection.setCorrectionActionType(correctionActionType);
        inspection.setCorrectionOldCoordinate("Freezer-A > Shelf-1 > Rack-1 > Box-2 > A1");
        if (correctionActionType != null) {
            inspection.setCorrectionNewCoordinate("MISSING".equals(expectedStatus) ? "Missing (not found during QC)"
                    : "Freezer-A > Shelf-1 > Rack-1 > Box-2 > B5");
            inspection.setCorrectionReason(correctionActionType + ": operator correction");
            inspection.setCorrectionByUser("7");
            inspection.setCorrectionTimestamp(new Timestamp(System.currentTimeMillis()));
        }
        return inspection;
    }

    @Test
    public void getQCStorageOverview_includesSamplesOnNonFreezerDevices() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("eligibleSamples", List.of(Map.of("bioSampleId", 18)));
        stubStorageOverviewFromPool(true, null, overview);

        ResponseEntity<Map<String, Object>> response = controller.getQCStorageOverview(null, null, null, null, true,
                null);

        assertNotNull(response.getBody());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> eligible = (List<Map<String, Object>>) response.getBody().get("eligibleSamples");
        assertEquals(1, eligible.size());
        assertEquals(18, eligible.get(0).get("bioSampleId"));
    }

    @Test
    public void getQCStorageOverview_countsOnlyBiorepositoryFlaggedDevices() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("counts", Map.of("freezers", 1, "shelves", 1, "racks", 1, "boxes", 1));
        overview.put("filters", Map.of("freezers", List.of("Bio-Device")));
        overview.put("biorepositoryScope",
                Map.of("deviceHierarchyBiorepositoryOnly", Boolean.TRUE, "includesAllActiveDeviceTypes", Boolean.FALSE));
        overview.put("eligibleSamples", List.of());
        stubStorageOverviewFromPool(true, null, overview);

        ResponseEntity<Map<String, Object>> response = controller.getQCStorageOverview(null, null, null, null, true,
                null);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        @SuppressWarnings("unchecked")
        Map<String, Object> counts = (Map<String, Object>) body.get("counts");
        assertEquals(1, counts.get("freezers"));
        assertEquals(1, counts.get("shelves"));
        assertEquals(1, counts.get("racks"));
        assertEquals(1, counts.get("boxes"));

        @SuppressWarnings("unchecked")
        Map<String, Object> filters = (Map<String, Object>) body.get("filters");
        @SuppressWarnings("unchecked")
        List<String> deviceOptions = (List<String>) filters.get("freezers");
        assertEquals(1, deviceOptions.size());
        assertEquals("Bio-Device", deviceOptions.get(0));

        @SuppressWarnings("unchecked")
        Map<String, Object> scope = (Map<String, Object>) body.get("biorepositoryScope");
        assertEquals(Boolean.TRUE, scope.get("deviceHierarchyBiorepositoryOnly"));
        assertEquals(Boolean.FALSE, scope.get("includesAllActiveDeviceTypes"));
    }

    @Test
    public void getQCStorageOverview_excludesSamplesOnNonBiorepositoryDevices() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("eligibleSamples", List.of(Map.of("bioSampleId", 100)));
        stubStorageOverviewFromPool(true, null, overview);

        ResponseEntity<Map<String, Object>> response = controller.getQCStorageOverview(null, null, null, null, true,
                null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> eligible = (List<Map<String, Object>>) response.getBody().get("eligibleSamples");
        assertEquals(1, eligible.size());
        assertEquals(100, eligible.get(0).get("bioSampleId"));
    }

    private void stubStorageOverviewFromPool(boolean includeInspected, Integer notebookId,
            Map<String, Object> overview) {
        when(qcSamplePoolService.buildStorageOverview(isNull(), isNull(), isNull(), isNull(), eq(includeInspected),
                eq(notebookId))).thenReturn(overview);
    }

    private StorageDevice buildDevice(int id, String name, boolean biorepositoryStorage) {
        StorageDevice device = new StorageDevice();
        device.setId(id);
        device.setName(name);
        device.setActive(true);
        device.setBiorepositoryStorage(biorepositoryStorage);
        return device;
    }

    private StorageShelf buildShelf(int id, String label, StorageDevice parentDevice) {
        StorageShelf shelf = new StorageShelf();
        shelf.setId(id);
        shelf.setLabel(label);
        shelf.setActive(true);
        shelf.setParentDevice(parentDevice);
        return shelf;
    }

    private StorageRack buildRack(int id, String label, StorageShelf parentShelf) {
        StorageRack rack = new StorageRack();
        rack.setId(id);
        rack.setLabel(label);
        rack.setActive(true);
        rack.setParentShelf(parentShelf);
        return rack;
    }

    private StorageBox buildBox(int id, String label, StorageRack parentRack) {
        StorageBox box = new StorageBox();
        box.setId(id);
        box.setLabel(label);
        box.setActive(true);
        box.setParentRack(parentRack);
        return box;
    }
}
