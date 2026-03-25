package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.UnitMapping;
import org.openelisglobal.common.service.BaseObjectService;

/**
 * Service interface for UnitMapping operations
 * 
 * Provides business logic for managing unit mappings with: - Conversion factor
 * validation (required when units don't match) - Unit mismatch handling
 */
public interface UnitMappingService extends BaseObjectService<UnitMapping, String> {

    /**
     * Create a new unit mapping with validation
     * 
     * @param mapping The mapping to create
     * @return The ID of the created mapping
     * @throws LIMSRuntimeException if unit mismatch without conversion factor
     */
    String createMapping(UnitMapping mapping);

    /**
     * Get all mappings for a specific analyzer field
     * 
     * @param analyzerFieldId The analyzer field ID
     * @return List of unit mappings
     */
    List<UnitMapping> getMappingsByAnalyzerFieldId(String analyzerFieldId);
}
