package org.openelisglobal.biorepository.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.BiorepositoryQCInspectionService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Biorepository QC Inspection operations.
 *
 * Provides endpoints for: - Fetching STORED samples with storage locations -
 * Creating QC inspection records - Bulk creating QC inspections - Retrieving QC
 * inspection history
 */
@RestController
@RequestMapping(value = "/rest/biorepository/qc-inspection")
public class BiorepositoryQCInspectionRestController extends BaseRestController {

    @Autowired
    private BiorepositoryQCInspectionService qcInspectionService;

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private SampleStorageService storageService;

    /**
     * Get all BioSamples with workflowStatus = STORED along with their storage
     * locations. This endpoint provides samples ready for QC inspection.
     */
    @GetMapping(value = "/samples", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getStoredSamplesForQC() {
        List<BioSample> storedSamples = bioSampleService.getAll().stream()
                .filter(bs -> WorkflowStatus.STORED.equals(bs.getWorkflowStatus())).toList();

        List<Map<String, Object>> result = new ArrayList<>();

        for (BioSample bioSample : storedSamples) {
            Map<String, Object> sampleData = new HashMap<>();

            // BioSample basic info
            sampleData.put("bioSampleId", bioSample.getId());
            sampleData.put("workflowStatus", bioSample.getWorkflowStatus().name());
            sampleData.put("biosafetyLevel", bioSample.getBiosafetyLevel().name());

            if (bioSample.getProjectId() != null) {
                sampleData.put("projectId", bioSample.getProjectId());
            }

            // SampleItem info
            SampleItem sampleItem = bioSample.getSampleItem();
            if (sampleItem != null) {
                sampleData.put("sampleItemId", sampleItem.getId());
                sampleData.put("externalId", sampleItem.getExternalId());

                if (sampleItem.getTypeOfSample() != null) {
                    sampleData.put("sampleType", sampleItem.getTypeOfSample().getDescription());
                    sampleData.put("sampleTypeId", sampleItem.getTypeOfSample().getId());
                }

                if (sampleItem.getSample() != null) {
                    sampleData.put("accessionNumber", sampleItem.getSample().getAccessionNumber());
                }

                // Get storage location
                Map<String, Object> location = storageService.getSampleItemLocation(sampleItem.getId().toString());
                if (location != null && !location.isEmpty()) {
                    sampleData.put("storageLocation", location);
                    // Extract hierarchical path for display
                    if (location.containsKey("hierarchicalPath")) {
                        sampleData.put("locationPath", location.get("hierarchicalPath"));
                    }
                }
            }

            // Check if sample has existing QC inspection
            BiorepositoryQCInspection mostRecent = qcInspectionService.getMostRecentByBioSampleId(bioSample.getId());
            if (mostRecent != null) {
                sampleData.put("lastQCInspection", mapQCInspection(mostRecent));
            }

            result.add(sampleData);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Bulk create QC inspections for multiple samples.
     */
    @PostMapping(value = "/bulk-apply", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> bulkApplyQC(@RequestBody BulkQCInspectionRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            // Validate required fields
            if (request.getBioSampleIds() == null || request.getBioSampleIds().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "At least one biosample ID is required"));
            }
            if (request.getInspectorName() == null || request.getInspectorName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Inspector name is required"));
            }

            // Use current timestamp if not provided
            Timestamp inspectionDate = request.getInspectionDate() != null ? request.getInspectionDate()
                    : new Timestamp(System.currentTimeMillis());

            List<BiorepositoryQCInspection> inspections = qcInspectionService.createBulkInspections(
                    request.getBioSampleIds(), request.getInspectorName(), inspectionDate, request.isSamplePresent(),
                    request.isLabelIntegrity(), request.isContainerIntegrity(), request.isVolumeAppearanceAcceptable(),
                    request.isCorrectPosition(), request.getDiscrepancyType(), request.getCorrectiveAction(),
                    request.getRemarks(), request.getQcBatchId(), request.getExpectedCoordinateSnapshot(), sysUserId);

            List<Map<String, Object>> result = new ArrayList<>();
            for (BiorepositoryQCInspection inspection : inspections) {
                result.add(mapQCInspection(inspection));
            }

            return ResponseEntity.ok(Map.of("inspections", result, "count", inspections.size()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create QC inspections: " + e.getMessage()));
        }
    }

    /**
     * Generate a random QC round (N boxes x M samples per box) from current stored
     * inventory.
     */
    @PostMapping(value = "/generate-round", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> generateQCRound(@RequestBody(required = false) GenerateQCRoundRequest request,
            HttpServletRequest httpRequest) {
        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        int boxesPerRound = request != null ? request.getBoxesPerRound() : 10;
        int samplesPerBox = request != null ? request.getSamplesPerBox() : 3;
        Long seed = request != null ? request.getSeed() : null;

        try {
            Map<String, Object> generated = qcInspectionService.generateRandomQCRound(boxesPerRound, samplesPerBox,
                    seed, sysUserId);
            return ResponseEntity.ok(generated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate QC round: " + e.getMessage()));
        }
    }

    /**
     * Apply corrective action for a failed QC inspection and capture coordinate
     * audit details.
     */
    @PostMapping(value = "/{inspectionId}/corrective-action", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> applyCorrectiveAction(@PathVariable("inspectionId") Integer inspectionId,
            @RequestBody CorrectiveActionRequest request, HttpServletRequest httpRequest) {
        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        if (request == null || request.getReason() == null || request.getReason().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Corrective reason is required"));
        }

        try {
            Map<String, Object> result = qcInspectionService.applyCorrectiveAction(inspectionId,
                    request.getObservedCoordinate(), request.getCorrectedCoordinate(), request.getReason(), sysUserId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to apply corrective action: " + e.getMessage()));
        }
    }

    /**
     * Get QC inspection history for a specific biosample.
     */
    @GetMapping(value = "/history/{bioSampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getQCHistory(@PathVariable("bioSampleId") Integer bioSampleId) {

        List<BiorepositoryQCInspection> inspections = qcInspectionService.getByBioSampleId(bioSampleId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (BiorepositoryQCInspection inspection : inspections) {
            result.add(mapQCInspection(inspection));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get QC statistics summary.
     */
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getQCStats() {
        long verifiedCount = qcInspectionService.countByQCResult(BiorepositoryQCInspection.QCResult.VERIFIED);
        long discrepancyCount = qcInspectionService
                .countByQCResult(BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalInspections", verifiedCount + discrepancyCount);
        stats.put("verified", verifiedCount);
        stats.put("discrepanciesFound", discrepancyCount);

        return ResponseEntity.ok(stats);
    }

    // ========== Helper Methods ==========

    private Map<String, Object> mapQCInspection(BiorepositoryQCInspection inspection) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", inspection.getId());
        map.put("bioSampleId", inspection.getBioSample().getId());
        map.put("inspectorName", inspection.getInspectorName());
        map.put("inspectionDate", inspection.getInspectionDate().toString());
        map.put("qcResult", inspection.getQcResult().name());

        // Checklist items
        map.put("samplePresent", inspection.isSamplePresent());
        map.put("labelIntegrity", inspection.isLabelIntegrity());
        map.put("containerIntegrity", inspection.isContainerIntegrity());
        map.put("volumeAppearanceAcceptable", inspection.isVolumeAppearanceAcceptable());
        map.put("correctPosition", inspection.isCorrectPosition());

        // Discrepancy details (if applicable)
        if (inspection.getDiscrepancyType() != null) {
            map.put("discrepancyType", inspection.getDiscrepancyType().name());
        }
        if (inspection.getCorrectiveAction() != null) {
            map.put("correctiveAction", inspection.getCorrectiveAction());
        }
        if (inspection.getRemarks() != null) {
            map.put("remarks", inspection.getRemarks());
        }
        if (inspection.getQcBatchId() != null) {
            map.put("qcBatchId", inspection.getQcBatchId());
        }
        if (inspection.getExpectedCoordinateSnapshot() != null) {
            map.put("expectedCoordinateSnapshot", inspection.getExpectedCoordinateSnapshot());
        }

        return map;
    }

    // ========== Request DTOs ==========

    public static class BulkQCInspectionRequest {
        private List<Integer> bioSampleIds;
        private String inspectorName;
        private Timestamp inspectionDate;
        private boolean samplePresent;
        private boolean labelIntegrity;
        private boolean containerIntegrity;
        private boolean volumeAppearanceAcceptable;
        private boolean correctPosition;
        private String discrepancyType;
        private String correctiveAction;
        private String remarks;
        private String qcBatchId;
        private String expectedCoordinateSnapshot;

        // Getters and setters
        public List<Integer> getBioSampleIds() {
            return bioSampleIds;
        }

        public void setBioSampleIds(List<Integer> bioSampleIds) {
            this.bioSampleIds = bioSampleIds;
        }

        public String getInspectorName() {
            return inspectorName;
        }

        public void setInspectorName(String inspectorName) {
            this.inspectorName = inspectorName;
        }

        public Timestamp getInspectionDate() {
            return inspectionDate;
        }

        public void setInspectionDate(Timestamp inspectionDate) {
            this.inspectionDate = inspectionDate;
        }

        public boolean isSamplePresent() {
            return samplePresent;
        }

        public void setSamplePresent(boolean samplePresent) {
            this.samplePresent = samplePresent;
        }

        public boolean isLabelIntegrity() {
            return labelIntegrity;
        }

        public void setLabelIntegrity(boolean labelIntegrity) {
            this.labelIntegrity = labelIntegrity;
        }

        public boolean isContainerIntegrity() {
            return containerIntegrity;
        }

        public void setContainerIntegrity(boolean containerIntegrity) {
            this.containerIntegrity = containerIntegrity;
        }

        public boolean isVolumeAppearanceAcceptable() {
            return volumeAppearanceAcceptable;
        }

        public void setVolumeAppearanceAcceptable(boolean volumeAppearanceAcceptable) {
            this.volumeAppearanceAcceptable = volumeAppearanceAcceptable;
        }

        public boolean isCorrectPosition() {
            return correctPosition;
        }

        public void setCorrectPosition(boolean correctPosition) {
            this.correctPosition = correctPosition;
        }

        public String getDiscrepancyType() {
            return discrepancyType;
        }

        public void setDiscrepancyType(String discrepancyType) {
            this.discrepancyType = discrepancyType;
        }

        public String getCorrectiveAction() {
            return correctiveAction;
        }

        public void setCorrectiveAction(String correctiveAction) {
            this.correctiveAction = correctiveAction;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }

        public String getQcBatchId() {
            return qcBatchId;
        }

        public void setQcBatchId(String qcBatchId) {
            this.qcBatchId = qcBatchId;
        }

        public String getExpectedCoordinateSnapshot() {
            return expectedCoordinateSnapshot;
        }

        public void setExpectedCoordinateSnapshot(String expectedCoordinateSnapshot) {
            this.expectedCoordinateSnapshot = expectedCoordinateSnapshot;
        }
    }

    public static class GenerateQCRoundRequest {
        private int boxesPerRound = 10;
        private int samplesPerBox = 3;
        private Long seed;

        public int getBoxesPerRound() {
            return boxesPerRound;
        }

        public void setBoxesPerRound(int boxesPerRound) {
            this.boxesPerRound = boxesPerRound;
        }

        public int getSamplesPerBox() {
            return samplesPerBox;
        }

        public void setSamplesPerBox(int samplesPerBox) {
            this.samplesPerBox = samplesPerBox;
        }

        public Long getSeed() {
            return seed;
        }

        public void setSeed(Long seed) {
            this.seed = seed;
        }
    }

    public static class CorrectiveActionRequest {
        private String observedCoordinate;
        private String correctedCoordinate;
        private String reason;

        public String getObservedCoordinate() {
            return observedCoordinate;
        }

        public void setObservedCoordinate(String observedCoordinate) {
            this.observedCoordinate = observedCoordinate;
        }

        public String getCorrectedCoordinate() {
            return correctedCoordinate;
        }

        public void setCorrectedCoordinate(String correctedCoordinate) {
            this.correctedCoordinate = correctedCoordinate;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
