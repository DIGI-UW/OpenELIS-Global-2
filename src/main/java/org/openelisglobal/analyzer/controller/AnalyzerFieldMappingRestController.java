package org.openelisglobal.analyzer.controller;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.form.AnalyzerFieldMappingForm;
import org.openelisglobal.analyzer.form.MappingPreviewForm;
import org.openelisglobal.analyzer.service.ActivationValidationResult;
import org.openelisglobal.analyzer.service.AnalyzerFieldMappingService;
import org.openelisglobal.analyzer.service.AnalyzerFieldService;
import org.openelisglobal.analyzer.service.AnalyzerMappingCopyService;
import org.openelisglobal.analyzer.service.AnalyzerMappingPreviewService;
import org.openelisglobal.analyzer.service.CopyMappingsResult;
import org.openelisglobal.analyzer.service.CopyOptions;
import org.openelisglobal.analyzer.service.MappingPreviewResult;
import org.openelisglobal.analyzer.service.MappingValidationService;
import org.openelisglobal.analyzer.service.PreviewOptions;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Analyzer Field Mapping management Handles CRUD operations
 * for field mappings between analyzer fields and OpenELIS fields
 */
@RestController
@RequestMapping("/rest/analyzer")
public class AnalyzerFieldMappingRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerFieldMappingRestController.class);

    @Autowired
    private AnalyzerFieldMappingService analyzerFieldMappingService;

    @Autowired
    private AnalyzerFieldService analyzerFieldService;

    @Autowired(required = false)
    private AnalyzerMappingCopyService analyzerMappingCopyService;

    @Autowired(required = false)
    private AnalyzerMappingPreviewService analyzerMappingPreviewService;

    @Autowired(required = false)
    private MappingValidationService mappingValidationService;

    @Autowired(required = false)
    private org.openelisglobal.analyzer.service.ValidationRuleConfigurationService validationRuleConfigurationService;

    @Autowired(required = false)
    private org.openelisglobal.analyzer.service.ValidationRuleEngine validationRuleEngine;

    /**
     * GET /rest/analyzer/analyzers/{analyzerId}/mappings Retrieve all field
     * mappings for an analyzer Service layer compiles all data - controller never
     * accesses relationships
     * 
     * Task Reference: T200 - Enhanced to support includeRetired parameter
     */
    @GetMapping("/analyzers/{analyzerId}/mappings")
    public ResponseEntity<List<Map<String, Object>>> getMappings(@PathVariable String analyzerId,
            @RequestParam(defaultValue = "false") boolean includeRetired) {
        try {
            // Service layer eagerly fetches all relationships and compiles complete data
            List<Map<String, Object>> response = analyzerFieldMappingService.getMappingsForAnalyzer(analyzerId,
                    includeRetired);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving mappings for analyzer: " + analyzerId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * POST /rest/analyzer/analyzers/{analyzerId}/mappings Create a new field
     * mapping Service layer handles all validation and data compilation -
     * controller never accesses relationships
     */
    @PostMapping("/analyzers/{analyzerId}/mappings")
    public ResponseEntity<Map<String, Object>> createMapping(@PathVariable String analyzerId,
            @Valid @RequestBody AnalyzerFieldMappingForm form) {
        try {
            // Service layer verifies analyzer field belongs to analyzer, validates type
            // compatibility,
            // and returns complete compiled data - controller never accesses relationships
            Map<String, Object> response = analyzerFieldMappingService.createMappingForAnalyzer(analyzerId,
                    form.getAnalyzerFieldId(), form.getOpenelisFieldId(), form.getOpenelisFieldType(),
                    form.getMappingType(), form.getIsRequired(), form.getIsActive(), form.getSpecimenTypeConstraint(),
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
     * PUT /rest/analyzer/analyzers/{analyzerId}/mappings/{mappingId} Update an
     * existing field mapping Service layer verifies ownership and compiles data -
     * controller never accesses relationships
     */
    @PutMapping("/analyzers/{analyzerId}/mappings/{mappingId}")
    public ResponseEntity<Map<String, Object>> updateMapping(@PathVariable String analyzerId,
            @PathVariable String mappingId, @Valid @RequestBody AnalyzerFieldMappingForm form) {
        try {
            // Verify mapping belongs to analyzer (service layer handles relationship
            // access)
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

            // Service layer compiles complete data - controller never accesses
            // relationships
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
     * DELETE /rest/analyzer/analyzers/{analyzerId}/mappings/{mappingId} Delete a
     * field mapping Service layer verifies ownership - controller never accesses
     * relationships
     */
    @DeleteMapping("/analyzers/{analyzerId}/mappings/{mappingId}")
    public ResponseEntity<Void> deleteMapping(@PathVariable String analyzerId, @PathVariable String mappingId) {
        try {
            // Verify mapping belongs to analyzer (service layer handles relationship
            // access)
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

    /**
     * POST /rest/analyzer/analyzers/{id}/activate-mappings Activate mapping changes
     * for an analyzer with validation Task Reference: T166
     * 
     * Validates activation requirements: - Required mappings present (Sample ID,
     * Test Code, Result Value) - Pending messages in error queue - Concurrent edit
     * detection
     */
    @PostMapping("/analyzers/{id}/activate-mappings")
    public ResponseEntity<Map<String, Object>> activateMappings(@PathVariable String id,
            @RequestBody Map<String, Object> request) {
        try {
            // Validate activation requirements
            ActivationValidationResult validationResult = analyzerFieldMappingService.validateActivation(id);

            // Check if activation can proceed
            if (!validationResult.isCanActivate()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Cannot activate: Required mappings missing");
                error.put("missingRequired", validationResult.getMissingRequired());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Get mapping IDs from request (optional - if not provided, activate all draft
            // mappings)
            @SuppressWarnings("unchecked")
            List<String> mappingIds = (List<String>) request.get("mappingIds");
            String confirmationToken = (String) request.get("confirmationToken");

            // If mappingIds provided, activate specific mappings
            int activatedCount = 0;
            if (mappingIds != null && !mappingIds.isEmpty()) {
                for (String mappingId : mappingIds) {
                    try {
                        // Verify mapping belongs to analyzer
                        if (analyzerFieldMappingService.verifyMappingBelongsToAnalyzer(mappingId, id)) {
                            // Activate mapping (confirmation token indicates user confirmed)
                            boolean confirmed = confirmationToken != null && !confirmationToken.isEmpty();
                            analyzerFieldMappingService.activateMapping(mappingId, confirmed);
                            activatedCount++;
                        }
                    } catch (LIMSRuntimeException e) {
                        // Check for optimistic locking exception (T168a)
                        if (e.getCause() instanceof org.hibernate.StaleObjectStateException) {
                            logger.warn("Concurrent edit detected for mapping " + mappingId);
                            Map<String, Object> error = new HashMap<>();
                            error.put("error", "Concurrent edit detected");
                            error.put("message", "Mapping was modified by another user. Please reload and try again.");
                            error.put("mappingId", mappingId);
                            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
                        }
                        logger.warn("Failed to activate mapping " + mappingId + ": " + e.getMessage());
                        // Continue with other mappings
                    }
                }
            }

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("activatedCount", activatedCount);
            response.put("warnings", validationResult.getWarnings());
            response.put("requiresAdditionalConfirmation", validationResult.getPendingMessagesCount() > 0);

            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error activating mappings: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            // Check for optimistic locking exception (T168a)
            if (e.getCause() instanceof org.hibernate.StaleObjectStateException) {
                error.put("message", "Mapping was modified by another user. Please reload and try again.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }
            // Check if it's a concurrent edit (would need specific exception type)
            if (e.getMessage() != null && e.getMessage().contains("concurrent")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error activating mappings", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /rest/analyzer/analyzers/{targetId}/copy-mappings Copy field mappings
     * from source analyzer to target analyzer Task Reference: T194
     * 
     * Request body: { sourceAnalyzerId: String, overwriteExisting: Boolean (default
     * true), skipIncompatible: Boolean (default false) }
     */
    @PostMapping("/analyzers/{targetId}/copy-mappings")
    public ResponseEntity<Map<String, Object>> copyMappings(@PathVariable String targetId,
            @RequestBody Map<String, Object> request) {
        try {
            if (analyzerMappingCopyService == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Copy mappings service not available");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }

            String sourceAnalyzerId = (String) request.get("sourceAnalyzerId");
            if (sourceAnalyzerId == null || sourceAnalyzerId.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "sourceAnalyzerId is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Build copy options
            CopyOptions options = new CopyOptions();
            if (request.containsKey("overwriteExisting")) {
                options.setOverwriteExisting((Boolean) request.get("overwriteExisting"));
            }
            if (request.containsKey("skipIncompatible")) {
                options.setSkipIncompatible((Boolean) request.get("skipIncompatible"));
            }

            // Perform copy operation
            CopyMappingsResult result = analyzerMappingCopyService.copyMappings(sourceAnalyzerId, targetId, options);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("copiedCount", result.getCopiedCount());
            response.put("skippedCount", result.getSkippedCount());
            response.put("warnings", result.getWarnings());

            // Convert conflicts to map format
            List<Map<String, Object>> conflictsList = new ArrayList<>();
            for (CopyMappingsResult.ConflictDetail conflict : result.getConflicts()) {
                Map<String, Object> conflictMap = new HashMap<>();
                conflictMap.put("fieldName", conflict.getFieldName());
                conflictMap.put("conflictType", conflict.getConflictType());
                conflictMap.put("message", conflict.getMessage());
                conflictsList.add(conflictMap);
            }
            response.put("conflicts", conflictsList);

            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error copying mappings: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error copying mappings", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            // Check if it's a rollback scenario (would need specific exception type)
            if (e.getMessage() != null && e.getMessage().contains("rollback")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /rest/analyzer/analyzers/{id}/preview-mapping Preview how a sample ASTM
     * message will be interpreted with current mappings
     * 
     * Task Reference: T157
     * 
     * Request body: { astmMessage: String (max 10KB), includeDetailedParsing:
     * Boolean, validateAllMappings: Boolean }
     * 
     * Response: { parsedFields: [...], appliedMappings: [...], entityPreview:
     * {...}, warnings: [...], errors: [...] }
     */
    @PostMapping("/analyzers/{id}/preview-mapping")
    public ResponseEntity<Map<String, Object>> previewMapping(@PathVariable String id,
            @Valid @RequestBody MappingPreviewForm form) {
        try {
            if (analyzerMappingPreviewService == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Mapping preview service not available");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }

            // Validate message size
            if (form.getAstmMessage() == null || form.getAstmMessage().length() > 10 * 1024) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "ASTM message exceeds maximum size of 10KB");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Build preview options
            PreviewOptions options = new PreviewOptions();
            options.setIncludeDetailedParsing(form.isIncludeDetailedParsing());
            options.setValidateAllMappings(form.isValidateAllMappings());

            // Call service to preview mapping
            MappingPreviewResult result = analyzerMappingPreviewService.previewMapping(id, form.getAstmMessage(),
                    options);

            // Convert result to response map
            Map<String, Object> response = new HashMap<>();
            response.put("parsedFields", result.getParsedFields());
            response.put("appliedMappings", result.getAppliedMappings());
            response.put("entityPreview", result.getEntityPreview());
            response.put("warnings", result.getWarnings());
            response.put("errors", result.getErrors());

            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error previewing mapping: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error previewing mapping", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error processing ASTM message: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /rest/analyzer/analyzers/{id}/validation-metrics Get validation metrics
     * for an analyzer
     * 
     * Task Reference: T206
     * 
     * Response: { accuracy: Float (0.0-1.0), unmappedCount: Integer,
     * unmappedFields: String[], warnings: String[], coverageByTestUnit: Map<String,
     * Float> }
     * 
     * Authorization: Requires analyzer view permissions Caching: Cache metrics for
     * 5 minutes (invalidate on mapping changes)
     */
    @GetMapping("/analyzers/{id}/validation-metrics")
    public ResponseEntity<Map<String, Object>> getValidationMetrics(@PathVariable String id) {
        try {
            if (mappingValidationService == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Mapping validation service not available");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }

            // Get validation metrics
            MappingValidationService.ValidationMetrics metrics = mappingValidationService.getValidationMetrics(id);

            // Convert to response map
            Map<String, Object> response = new HashMap<>();
            response.put("accuracy", metrics.getAccuracy());
            response.put("unmappedCount", metrics.getUnmappedCount());
            response.put("unmappedFields", metrics.getUnmappedFields());
            response.put("warnings", metrics.getWarnings());
            response.put("coverageByTestUnit", metrics.getCoverageByTestUnit());

            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error retrieving validation metrics: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error retrieving validation metrics", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error retrieving validation metrics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /rest/analyzer/analyzers/{analyzerId}/fields/{fieldId}/validate-value
     * Validate a field value against custom field type validation rules
     * 
     * Task Reference: T176
     * 
     * @param analyzerId The analyzer ID
     * @param fieldId    The analyzer field ID
     * @param request    Request body containing "value" to validate
     * @return Validation result with isValid flag and error messages
     */
    @PostMapping("/analyzers/{analyzerId}/fields/{fieldId}/validate-value")
    public ResponseEntity<Map<String, Object>> validateFieldValue(@PathVariable String analyzerId,
            @PathVariable String fieldId, @RequestBody Map<String, Object> request) {
        try {
            String value = (String) request.get("value");
            if (value == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Value is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Get analyzer field
            org.openelisglobal.analyzer.valueholder.AnalyzerField field = analyzerFieldService.get(fieldId);
            if (field == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Analyzer field not found: " + fieldId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // Verify field belongs to analyzer
            if (field.getAnalyzer() == null || !field.getAnalyzer().getId().equals(analyzerId)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Field does not belong to analyzer");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Only validate if field type is CUSTOM
            if (field.getFieldType() != org.openelisglobal.analyzer.valueholder.AnalyzerField.FieldType.CUSTOM) {
                Map<String, Object> result = new HashMap<>();
                result.put("isValid", true);
                result.put("message", "Field type is not CUSTOM, no validation rules apply");
                return ResponseEntity.ok(result);
            }

            // Get custom field type ID (T141)
            String customFieldTypeId = field.getCustomFieldTypeId();
            if (customFieldTypeId == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("isValid", true);
                result.put("message", "No custom field type associated with this field");
                return ResponseEntity.ok(result);
            }

            // Fetch validation rules
            if (validationRuleConfigurationService == null || validationRuleEngine == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("isValid", true);
                result.put("message", "Validation rule services not available");
                return ResponseEntity.ok(result);
            }

            // Evaluate validation rules
            List<org.openelisglobal.analyzer.valueholder.ValidationRuleConfiguration> rules = validationRuleConfigurationService
                    .findActiveRulesByCustomFieldTypeId(customFieldTypeId);

            List<String> errors = new ArrayList<>();
            boolean isValid = true;

            for (org.openelisglobal.analyzer.valueholder.ValidationRuleConfiguration rule : rules) {
                try {
                    boolean ruleValid = validationRuleEngine.evaluateRule(value, rule);
                    if (!ruleValid) {
                        isValid = false;
                        String errorMsg = rule.getErrorMessage() != null ? rule.getErrorMessage()
                                : "Value does not meet validation rule: " + rule.getRuleName();
                        errors.add(errorMsg);
                    }
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid validation rule expression for rule {}: {}", rule.getId(), e.getMessage());
                    // Skip invalid rules
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("isValid", isValid);
            result.put("errors", errors);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error validating field value", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
