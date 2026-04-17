package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.form.CustomFieldTypeForm;
import org.openelisglobal.analyzer.form.ValidationRuleConfigurationForm;
import org.openelisglobal.analyzer.service.CustomFieldTypeService;
import org.openelisglobal.analyzer.service.ValidationRuleConfigurationService;
import org.openelisglobal.analyzer.valueholder.CustomFieldType;
import org.openelisglobal.analyzer.valueholder.ValidationRuleConfiguration;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.dao.UserModuleService;
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
 * REST Controller for Custom Field Type management Handles CRUD operations for
 * custom field types (FR-018)
 * 
 */
@RestController
@RequestMapping("/rest/analyzer/custom-field-types")
public class CustomFieldTypeRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(CustomFieldTypeRestController.class);

    @Autowired
    private CustomFieldTypeService customFieldTypeService;

    @Autowired
    private ValidationRuleConfigurationService validationRuleConfigurationService;

    @Autowired
    private UserModuleService userModuleService;

    /**
     * GET /rest/analyzer/custom-field-types Retrieve all custom field types
     */
    @GetMapping
    public ResponseEntity<List<CustomFieldType>> getAllCustomFieldTypes() {
        try {
            List<CustomFieldType> types = customFieldTypeService.getAll();
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            logger.error("Error retrieving custom field types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /rest/analyzer/custom-field-types/active Retrieve all active custom field
     * types
     */
    @GetMapping("/active")
    public ResponseEntity<List<CustomFieldType>> getActiveCustomFieldTypes() {
        try {
            List<CustomFieldType> types = customFieldTypeService.getAllActiveTypes();
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            logger.error("Error retrieving active custom field types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /rest/analyzer/custom-field-types/{id} Retrieve a specific custom field
     * type by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomFieldType> getCustomFieldType(@PathVariable String id) {
        try {
            CustomFieldType type = customFieldTypeService.get(id);
            if (type != null) {
                return ResponseEntity.ok(type);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error retrieving custom field type: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /rest/analyzer/custom-field-types Create a new custom field type
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createCustomFieldType(@Valid @RequestBody CustomFieldTypeForm form) {
        try {
            CustomFieldType customFieldType = mapFormToEntity(form);
            CustomFieldType created = customFieldTypeService.createCustomFieldType(customFieldType);

            Map<String, Object> response = new HashMap<>();
            response.put("id", created.getId());
            response.put("message", "Custom field type created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error creating custom field type", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Unexpected error creating custom field type", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to create custom field type");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * PUT /rest/analyzer/custom-field-types/{id} Update an existing custom field
     * type
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCustomFieldType(@PathVariable String id,
            @Valid @RequestBody CustomFieldTypeForm form) {
        try {
            CustomFieldType existing = customFieldTypeService.get(id);
            if (existing == null) {
                throw new LIMSRuntimeException("Custom field type not found: " + id);
            }

            CustomFieldType updated = mapFormToEntity(form);
            updated.setId(id);
            updated = customFieldTypeService.updateCustomFieldType(updated);

            Map<String, Object> response = new HashMap<>();
            response.put("id", updated.getId());
            response.put("message", "Custom field type updated successfully");
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error updating custom field type: " + id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Unexpected error updating custom field type: " + id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to update custom field type");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * DELETE /rest/analyzer/custom-field-types/{id} Delete a custom field type
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCustomFieldType(@PathVariable String id) {
        try {
            CustomFieldType existing = customFieldTypeService.get(id);
            if (existing == null) {
                throw new LIMSRuntimeException("Custom field type not found: " + id);
            }

            customFieldTypeService.delete(existing);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Custom field type deleted successfully");
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error deleting custom field type: " + id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Unexpected error deleting custom field type: " + id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to delete custom field type");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /rest/analyzer/custom-field-types/{id}/validation-rules Retrieve all
     * validation rules for a custom field type
     * 
     */
    @GetMapping("/{id}/validation-rules")
    public ResponseEntity<List<ValidationRuleConfiguration>> getValidationRules(@PathVariable String id) {
        try {
            // Verify custom field type exists
            CustomFieldType customFieldType = customFieldTypeService.get(id);
            if (customFieldType == null) {
                return ResponseEntity.notFound().build();
            }

            List<ValidationRuleConfiguration> rules = validationRuleConfigurationService.findByCustomFieldTypeId(id);
            return ResponseEntity.ok(rules);
        } catch (Exception e) {
            logger.error("Error retrieving validation rules for custom field type: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /rest/analyzer/custom-field-types/{id}/validation-rules Create a new
     * validation rule for a custom field type
     * 
     * Authorization: System Administrator only
     */
    @PostMapping("/{id}/validation-rules")
    public ResponseEntity<Map<String, Object>> createValidationRule(@PathVariable String id,
            @Valid @RequestBody ValidationRuleConfigurationForm form, HttpServletRequest request) {
        try {
            if (!userModuleService.isUserAdmin(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Administrator access required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            // Verify custom field type exists
            CustomFieldType customFieldType = customFieldTypeService.get(id);
            if (customFieldType == null) {
                throw new LIMSRuntimeException("Custom field type not found: " + id);
            }

            ValidationRuleConfiguration rule = mapFormToValidationRule(form, customFieldType);
            ValidationRuleConfiguration created = validationRuleConfigurationService.createValidationRule(rule);

            Map<String, Object> response = new HashMap<>();
            response.put("id", created.getId());
            response.put("message", "Validation rule created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error creating validation rule", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Unexpected error creating validation rule", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to create validation rule");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * PUT /rest/analyzer/custom-field-types/{id}/validation-rules/{ruleId} Update
     * an existing validation rule
     * 
     * Authorization: System Administrator only
     */
    @PutMapping("/{id}/validation-rules/{ruleId}")
    public ResponseEntity<Map<String, Object>> updateValidationRule(@PathVariable String id,
            @PathVariable String ruleId, @Valid @RequestBody ValidationRuleConfigurationForm form,
            HttpServletRequest request) {
        try {
            if (!userModuleService.isUserAdmin(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Administrator access required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            // Verify custom field type exists
            CustomFieldType customFieldType = customFieldTypeService.get(id);
            if (customFieldType == null) {
                throw new LIMSRuntimeException("Custom field type not found: " + id);
            }

            // Verify rule exists and belongs to this custom field type
            ValidationRuleConfiguration existing = validationRuleConfigurationService.get(ruleId);
            if (existing == null || !existing.getCustomFieldType().getId().equals(id)) {
                throw new LIMSRuntimeException("Validation rule not found: " + ruleId);
            }

            ValidationRuleConfiguration rule = mapFormToValidationRule(form, customFieldType);
            rule.setId(ruleId);
            ValidationRuleConfiguration updated = validationRuleConfigurationService.updateValidationRule(rule);

            Map<String, Object> response = new HashMap<>();
            response.put("id", updated.getId());
            response.put("message", "Validation rule updated successfully");
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error updating validation rule: " + ruleId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Unexpected error updating validation rule: " + ruleId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to update validation rule");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * DELETE /rest/analyzer/custom-field-types/{id}/validation-rules/{ruleId}
     * Delete a validation rule
     * 
     * Authorization: System Administrator only
     */
    @DeleteMapping("/{id}/validation-rules/{ruleId}")
    public ResponseEntity<Map<String, Object>> deleteValidationRule(@PathVariable String id,
            @PathVariable String ruleId, HttpServletRequest request) {
        try {
            if (!userModuleService.isUserAdmin(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Administrator access required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            // Verify custom field type exists
            CustomFieldType customFieldType = customFieldTypeService.get(id);
            if (customFieldType == null) {
                throw new LIMSRuntimeException("Custom field type not found: " + id);
            }

            // Verify rule exists and belongs to this custom field type
            ValidationRuleConfiguration existing = validationRuleConfigurationService.get(ruleId);
            if (existing == null || !existing.getCustomFieldType().getId().equals(id)) {
                throw new LIMSRuntimeException("Validation rule not found: " + ruleId);
            }

            validationRuleConfigurationService.deleteValidationRule(existing);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Validation rule deleted successfully");
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error deleting validation rule: " + ruleId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Unexpected error deleting validation rule: " + ruleId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to delete validation rule");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Map form to entity
     */
    private CustomFieldType mapFormToEntity(CustomFieldTypeForm form) {
        CustomFieldType entity = new CustomFieldType();
        entity.setTypeName(form.getTypeName());
        entity.setDisplayName(form.getDisplayName());
        entity.setValidationPattern(form.getValidationPattern());
        entity.setValueRangeMin(form.getValueRangeMin() != null ? new BigDecimal(form.getValueRangeMin()) : null);
        entity.setValueRangeMax(form.getValueRangeMax() != null ? new BigDecimal(form.getValueRangeMax()) : null);
        entity.setAllowedCharacters(form.getAllowedCharacters());
        entity.setIsActive(form.getIsActive() != null ? form.getIsActive() : true);
        return entity;
    }

    /**
     * Map ValidationRuleConfigurationForm to ValidationRuleConfiguration entity
     */
    private ValidationRuleConfiguration mapFormToValidationRule(ValidationRuleConfigurationForm form,
            CustomFieldType customFieldType) {
        ValidationRuleConfiguration rule = new ValidationRuleConfiguration();
        if (form.getId() != null) {
            rule.setId(form.getId());
        }
        rule.setCustomFieldType(customFieldType);
        rule.setRuleName(form.getRuleName());
        rule.setRuleType(ValidationRuleConfiguration.RuleType.valueOf(form.getRuleType()));
        rule.setRuleExpression(form.getRuleExpression());
        rule.setErrorMessage(form.getErrorMessage());
        rule.setIsActive(form.getIsActive() != null ? form.getIsActive() : true);
        return rule;
    }
}
