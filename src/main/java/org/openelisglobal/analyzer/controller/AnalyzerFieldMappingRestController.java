package org.openelisglobal.analyzer.controller;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.analyzer.form.AnalyzerFieldMappingForm;
import org.openelisglobal.analyzer.service.AnalyzerFieldMappingService;
import org.openelisglobal.analyzer.service.AnalyzerFieldService;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.rest.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Analyzer Field Mapping management
 * Handles CRUD operations for field mappings between analyzer fields and OpenELIS fields
 */
@RestController
@RequestMapping("/rest/analyzer")
public class AnalyzerFieldMappingRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerFieldMappingRestController.class);

    @Autowired
    private AnalyzerFieldMappingService analyzerFieldMappingService;

    @Autowired
    private AnalyzerFieldService analyzerFieldService;

    /**
     * GET /rest/analyzer/analyzers/{analyzerId}/mappings
     * Retrieve all field mappings for an analyzer
     * Service layer compiles all data - controller never accesses relationships
     */
    @GetMapping("/analyzers/{analyzerId}/mappings")
    public ResponseEntity<List<Map<String, Object>>> getMappings(@PathVariable String analyzerId) {
        try {
            // Service layer eagerly fetches all relationships and compiles complete data
            List<Map<String, Object>> response = analyzerFieldMappingService.getMappingsForAnalyzer(analyzerId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving mappings for analyzer: " + analyzerId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * POST /rest/analyzer/analyzers/{analyzerId}/mappings
     * Create a new field mapping
     * Service layer handles all validation and data compilation - controller never accesses relationships
     */
    @PostMapping("/analyzers/{analyzerId}/mappings")
    public ResponseEntity<Map<String, Object>> createMapping(@PathVariable String analyzerId,
            @Valid @RequestBody AnalyzerFieldMappingForm form) {
        try {
            // Service layer verifies analyzer field belongs to analyzer, validates type compatibility,
            // and returns complete compiled data - controller never accesses relationships
            Map<String, Object> response = analyzerFieldMappingService.createMappingForAnalyzer(
                    analyzerId,
                    form.getAnalyzerFieldId(),
                    form.getOpenelisFieldId(),
                    form.getOpenelisFieldType(),
                    form.getMappingType(),
                    form.getIsRequired(),
                    form.getIsActive(),
                    form.getSpecimenTypeConstraint(),
                    form.getPanelConstraint());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error creating mapping: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error creating mapping", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * PUT /rest/analyzer/analyzers/{analyzerId}/mappings/{mappingId}
     * Update an existing field mapping
     * Service layer verifies ownership and compiles data - controller never accesses relationships
     */
    @PutMapping("/analyzers/{analyzerId}/mappings/{mappingId}")
    public ResponseEntity<Map<String, Object>> updateMapping(@PathVariable String analyzerId,
            @PathVariable String mappingId, @Valid @RequestBody AnalyzerFieldMappingForm form) {
        try {
            // Verify mapping belongs to analyzer (service layer handles relationship access)
            if (!analyzerFieldMappingService.verifyMappingBelongsToAnalyzer(mappingId, analyzerId)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Mapping does not belong to analyzer: " + analyzerId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Get existing mapping (for update - no relationship access)
            AnalyzerFieldMapping mapping;
            try {
                mapping = analyzerFieldMappingService.get(mappingId);
            } catch (org.hibernate.ObjectNotFoundException e) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Mapping not found: " + mappingId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // Update mapping fields (only direct properties, no relationship access)
            if (form.getOpenelisFieldId() != null) {
                mapping.setOpenelisFieldId(form.getOpenelisFieldId());
            }
            if (form.getOpenelisFieldType() != null) {
                mapping.setOpenelisFieldType(form.getOpenelisFieldType());
            }
            if (form.getMappingType() != null) {
                mapping.setMappingType(form.getMappingType());
            }
            if (form.getIsRequired() != null) {
                mapping.setIsRequired(form.getIsRequired());
            }
            if (form.getIsActive() != null) {
                mapping.setIsActive(form.getIsActive());
            }
            if (form.getSpecimenTypeConstraint() != null) {
                mapping.setSpecimenTypeConstraint(form.getSpecimenTypeConstraint());
            }
            if (form.getPanelConstraint() != null) {
                mapping.setPanelConstraint(form.getPanelConstraint());
            }

            analyzerFieldMappingService.update(mapping);

            // Service layer compiles complete data - controller never accesses relationships
            Map<String, Object> response = analyzerFieldMappingService.getMappingWithCompleteData(mappingId);
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error updating mapping: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error updating mapping", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * DELETE /rest/analyzer/analyzers/{analyzerId}/mappings/{mappingId}
     * Delete a field mapping
     * Service layer verifies ownership - controller never accesses relationships
     */
    @DeleteMapping("/analyzers/{analyzerId}/mappings/{mappingId}")
    public ResponseEntity<Void> deleteMapping(@PathVariable String analyzerId, @PathVariable String mappingId) {
        try {
            // Verify mapping belongs to analyzer (service layer handles relationship access)
            if (!analyzerFieldMappingService.verifyMappingBelongsToAnalyzer(mappingId, analyzerId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // Get existing mapping (for delete - no relationship access)
            AnalyzerFieldMapping mapping;
            try {
                mapping = analyzerFieldMappingService.get(mappingId);
            } catch (org.hibernate.ObjectNotFoundException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            analyzerFieldMappingService.delete(mapping);

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting mapping", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

