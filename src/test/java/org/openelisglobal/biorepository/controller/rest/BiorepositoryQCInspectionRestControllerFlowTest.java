package org.openelisglobal.biorepository.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
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
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.service.StorageLocationService;
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

        when(qcInspectionService.createBulkInspections(any(), any(), any(), any(Boolean.class), any(Boolean.class),
                any(Boolean.class), any(Boolean.class), any(Boolean.class), any(), any(), any(), eq("7")))
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

        when(qcInspectionService.createBulkInspections(any(), any(), any(), any(Boolean.class), any(Boolean.class),
                any(Boolean.class), any(Boolean.class), any(Boolean.class), any(), any(), any(), eq("7")))
                        .thenReturn(List.of(inspection));
        when(qcInspectionService.applyCorrectionWorkflow(eq(inspection), eq(correctionActionType), any(), any(), any(),
                any(), any(), any(), eq("7"))).thenReturn(Map.of(
                        "actionType", correctionActionType,
                        "reason", correctionActionType + ": operator correction",
                        "correctionTimestamp", inspection.getCorrectionTimestamp().toString(),
                        "updatedLocation", Map.of("hierarchicalPath", "Freezer-A > Shelf-1 > Rack-1 > Box-2",
                                "positionCoordinate", "B5")));

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
}
