package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.QualitativeResultMapping;
import org.openelisglobal.common.service.BaseObjectService;

/**
 * Service interface for QualitativeResultMapping operations
 * 
 * Provides business logic for managing qualitative result mappings with: -
 * Many-to-one mapping support (multiple analyzer values → single OpenELIS code)
 * - Duplicate value validation (unique constraint on analyzer_field_id +
 * analyzer_value)
 */
public interface QualitativeResultMappingService extends BaseObjectService<QualitativeResultMapping, String> {

    /**
     * Create a new qualitative result mapping with validation
     * 
     * @param mapping The mapping to create
     * @return The ID of the created mapping
     * @throws LIMSRuntimeException if duplicate value exists for same analyzer
     *                              field
     */
    String createMapping(QualitativeResultMapping mapping);

    /**
     * Get all mappings for a specific analyzer field
     * 
     * @param analyzerFieldId The analyzer field ID
     * @return List of qualitative result mappings
     */
    List<QualitativeResultMapping> getMappingsByAnalyzerFieldId(String analyzerFieldId);
}
