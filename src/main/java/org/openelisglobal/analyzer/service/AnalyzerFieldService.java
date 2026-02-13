package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.common.service.BaseObjectService;

/**
 * Service interface for AnalyzerField operations
 * 
 * Provides business logic for managing analyzer fields (test codes, units,
 * qualitative values)
 */
public interface AnalyzerFieldService extends BaseObjectService<AnalyzerField, String> {

    /**
     * Get all fields for a specific analyzer
     * 
     * @param analyzerId The analyzer ID
     * @return List of analyzer fields
     */
    List<AnalyzerField> getFieldsByAnalyzerId(String analyzerId);

    /**
     * Create a new analyzer field with validation
     * 
     * @param field The analyzer field to create
     * @return The ID of the created field
     * @throws LIMSRuntimeException if validation fails
     */
    String createField(AnalyzerField field);
}
