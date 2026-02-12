package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.analyzer.form.OpenELISFieldForm;
import org.openelisglobal.analyzer.service.OpenELISFieldService;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for creating OpenELIS fields inline from the analyzer mapping
 * interface.
 * 
 * Endpoint: POST /rest/analyzer/openelis-fields Authorization: LAB_ADMIN or
 * LAB_SUPERVISOR (TODO: Add security annotations)
 */
@RestController
@RequestMapping("/rest/analyzer")
public class OpenELISFieldRestController extends BaseRestController {

    @Autowired
    private OpenELISFieldService openELISFieldService;

    /**
     * Creates a new OpenELIS field.
     * 
     * @param form    The form containing field creation data
     * @param result  Binding result for validation errors
     * @param request HTTP request
     * @return ResponseEntity with created field data or error
     */
    @PostMapping("/openelis-fields")
    public ResponseEntity<?> createField(@RequestBody @Valid OpenELISFieldForm form, BindingResult bindingResult,
            HttpServletRequest request) {

        // Validate form (BindingResult must come immediately after @Valid parameter)
        if (bindingResult.hasErrors()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Validation failed");
            errorResponse.put("errors", bindingResult.getAllErrors());
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            // Additional validation check (in case @Valid didn't catch everything)
            if (form.getEntityType() == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Validation failed");
                errorResponse.put("message", "Entity type is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Validate uniqueness
            if (!openELISFieldService.validateFieldUniqueness(form)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Field with the same name, code, or LOINC already exists");
                errorResponse.put("message", getUniquenessErrorMessage(form));
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }

            // Create the field
            String fieldId = openELISFieldService.createField(form);

            // Get the created field data
            Map<String, Object> fieldData = openELISFieldService.getFieldById(fieldId, form.getEntityType());

            Map<String, Object> response = new HashMap<>();
            response.put("id", fieldId);
            response.put("field", fieldData);
            response.put("message", "Field '" + form.getFieldName() + "' created successfully and ready for mapping");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (LIMSRuntimeException e) {
            LogEvent.logError("Error creating OpenELIS field: " + e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error creating field");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            LogEvent.logError("Unexpected error creating OpenELIS field: " + e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Unexpected error");
            errorResponse.put("message", "An unexpected error occurred while creating the field");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Gets a field by ID and entity type.
     * 
     * @param fieldId    The ID of the field
     * @param entityType The entity type (TEST, PANEL, etc.)
     * @return ResponseEntity with field data or 404 if not found
     */
    @GetMapping("/openelis-fields/{fieldId}")
    public ResponseEntity<?> getField(@PathVariable String fieldId, @RequestParam(required = false) String entityType) {

        // Validate entity type before lookup
        if (entityType == null || entityType.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Entity type parameter is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        OpenELISFieldForm.EntityType type;
        try {
            type = OpenELISFieldForm.EntityType.valueOf(entityType.toUpperCase());
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Unsupported entity type: " + entityType);
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            Map<String, Object> fieldData = openELISFieldService.getFieldById(fieldId, type);

            if (fieldData == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(fieldData);

        } catch (NumberFormatException e) {
            // Invalid ID format (e.g., "INVALID-ID" when numeric ID expected)
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            // LIMSRuntimeException from DAO - likely field not found
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            // For any other exception, check if it's a "not found" type error
            String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (errorMessage.contains("not found") || errorMessage.contains("does not exist")
                    || errorMessage.contains("no such") || errorMessage.contains("for input string")
                    || e instanceof java.util.NoSuchElementException) {
                return ResponseEntity.notFound().build();
            }
            LogEvent.logError("Error retrieving OpenELIS field: " + e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error retrieving field");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Validates field uniqueness without creating the field.
     * 
     * @param form The form containing field data to validate
     * @return ResponseEntity with validation result
     */
    @PostMapping("/openelis-fields/validate")
    public ResponseEntity<?> validateField(@RequestBody @Valid OpenELISFieldForm form, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Validation failed");
            errorResponse.put("errors", bindingResult.getAllErrors());
            return ResponseEntity.badRequest().body(errorResponse);
        }

        boolean isUnique = openELISFieldService.validateFieldUniqueness(form);

        Map<String, Object> response = new HashMap<>();
        response.put("isUnique", isUnique);
        if (!isUnique) {
            response.put("message", getUniquenessErrorMessage(form));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Generates an appropriate error message based on the entity type and form
     * data.
     */
    private String getUniquenessErrorMessage(OpenELISFieldForm form) {
        switch (form.getEntityType()) {
        case TEST:
            if (form.getTestCode() != null) {
                return "Test code '" + form.getTestCode() + "' already exists";
            }
            return "Test with name '" + form.getFieldName() + "' already exists";
        case PANEL:
            return "Panel code '" + form.getPanelCode() + "' already exists";
        case SAMPLE:
            return "Sample type code '" + form.getSampleTypeCode() + "' already exists";
        case QC:
            return "Control lot number '" + form.getLotNumber() + "' already exists for '" + form.getControlName()
                    + "'";
        case METADATA:
            return "Field name '" + form.getFieldName() + "' already exists in metadata";
        case UNIT:
            return "Unit code '" + form.getUnitCode() + "' already exists";
        default:
            return "Field with the same name or code already exists";
        }
    }
}
