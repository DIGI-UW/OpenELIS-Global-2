package org.openelisglobal.storage.controller;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.form.SampleAssignmentForm;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.service.StorageDashboardService;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Sample Storage operations Handles sample assignment and
 * movement
 */
@RestController
@RequestMapping("/rest/storage/samples")
public class SampleStorageRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(SampleStorageRestController.class);

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Autowired
    private StorageDashboardService storageDashboardService;

    /**
     * Get all samples with storage assignments GET /rest/storage/samples
     * Supports filtering by location and status (FR-065)
     * 
     * @param countOnly If "true", returns metrics only
     * @param location  Optional location filter (hierarchical path substring)
     * @param status    Optional status filter (active, disposed, etc.)
     */
    @GetMapping("")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getSamples(
            @RequestParam(required = false) String countOnly,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String status) {
        try {
            if ("true".equals(countOnly)) {
                // Return count metrics only
                List<SampleStorageAssignment> allAssignments = sampleStorageAssignmentDAO.getAll();

                long totalSamples = allAssignments.size();
                long active = allAssignments.stream()
                        .filter(a -> a.getSample() != null && (a.getSample().getStatus() == null
                                || !"disposed".equalsIgnoreCase(a.getSample().getStatus())))
                        .count();
                long disposed = allAssignments.stream()
                        .filter(a -> a.getSample() != null && "disposed".equalsIgnoreCase(a.getSample().getStatus()))
                        .count();

                // Count unique storage locations (rooms, devices, shelves, racks)
                long storageLocations = storageLocationService.getRooms().size()
                        + storageLocationService.getAllDevices().size() + storageLocationService.getAllShelves().size()
                        + storageLocationService.getAllRacks().size();

                Map<String, Object> metrics = new HashMap<>();
                metrics.put("totalSamples", totalSamples);
                metrics.put("active", active);
                metrics.put("disposed", disposed);
                metrics.put("storageLocations", storageLocations);

                List<Map<String, Object>> response = new ArrayList<>();
                response.add(metrics);
                return ResponseEntity.ok(response);
            } else {
                // Apply filters if provided (FR-065: Samples tab - filter by location and status)
                List<Map<String, Object>> response;
                if (location != null || status != null) {
                    response = storageDashboardService.filterSamples(location, status);
                    logger.info("Returning {} filtered samples (location={}, status={})", response.size(), location,
                            status);
                } else {
                    // No filters - return all samples
                    response = sampleStorageService.getAllSamplesWithAssignments();
                    logger.info("Returning {} samples with storage assignments", response.size());
                }
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            logger.error("Error getting samples", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Assign sample to storage position POST /rest/storage/samples/assign
     */
    @PostMapping("/assign")
    public ResponseEntity<Map<String, Object>> assignSample(@Valid @RequestBody SampleAssignmentForm form) {
        try {
            // Validate required fields
            if (form.getSampleId() == null || form.getSampleId().trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "Sample ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            if (form.getPositionId() == null || form.getPositionId().trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "Position ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Service layer prepares all data including hierarchical path within
            // transaction
            Map<String, Object> response = sampleStorageService.assignSampleWithDetails(form.getSampleId(),
                    form.getPositionId(), form.getNotes());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "An error occurred during assignment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
