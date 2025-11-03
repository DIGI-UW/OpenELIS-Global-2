package org.openelisglobal.storage.controller;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.storage.form.SampleAssignmentForm;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.valueholder.StoragePosition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Sample Storage operations Handles sample assignment and
 * movement
 */
@RestController
@RequestMapping("/rest/storage/samples")
public class SampleStorageRestController extends BaseRestController {

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private StorageLocationService storageLocationService;

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

            String assignmentId = sampleStorageService.assignSample(form.getSampleId(), form.getPositionId(),
                    form.getNotes());

            // Build hierarchical path
            StoragePosition position = (StoragePosition) storageLocationService.get(form.getPositionId(),
                    StoragePosition.class);
            String hierarchicalPath = position != null ? storageLocationService.buildHierarchicalPath(position)
                    : "Unknown";

            Map<String, Object> response = new HashMap<>();
            response.put("assignmentId", assignmentId);
            response.put("hierarchicalPath", hierarchicalPath);
            response.put("assignedDate", new java.sql.Timestamp(System.currentTimeMillis()).toString());

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
