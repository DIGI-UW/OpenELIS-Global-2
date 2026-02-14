package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.form.OpenELISFieldForm;
import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * Service interface for creating OpenELIS fields inline from the analyzer
 * mapping interface.
 * 
 * Supports creation of TEST, PANEL, RESULT, ORDER, SAMPLE, QC, METADATA, UNIT
 * entities.
 */
public interface OpenELISFieldService {

    /**
     * Creates a new OpenELIS field based on the provided form data.
     * 
     * @param form The form containing field creation data
     * @return The ID of the created field
     * @throws LIMSRuntimeException if validation fails or creation fails
     */
    String createField(OpenELISFieldForm form) throws LIMSRuntimeException;

    /**
     * Validates field uniqueness based on entity type and field name/code.
     * 
     * @param form The form containing field data to validate
     * @return true if the field is unique, false otherwise
     */
    boolean validateFieldUniqueness(OpenELISFieldForm form);

    /**
     * Gets a field by its ID and entity type.
     * 
     * @param fieldId    The ID of the field
     * @param entityType The entity type (TEST, PANEL, etc.)
     * @return A map containing field data, or null if not found
     */
    java.util.Map<String, Object> getFieldById(String fieldId, OpenELISFieldForm.EntityType entityType);
}
