package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.common.service.BaseObjectService;

/**
 * Service interface for AnalyzerFieldMapping operations
 * 
 * Provides business logic for managing field mappings with: - Type
 * compatibility validation (numeric→numeric, qualitative→qualitative,
 * text→text) - Required mapping validation (Sample ID, Test Code, Result Value)
 * - Draft/active workflow with activation confirmation
 */
public interface AnalyzerFieldMappingService extends BaseObjectService<AnalyzerFieldMapping, String> {

    /**
     * Create a new field mapping with validation
     * 
     * @param mapping The mapping to create
     * @return The ID of the created mapping
     * @throws LIMSRuntimeException if validation fails (type incompatibility)
     */
    String createMapping(AnalyzerFieldMapping mapping);

    /**
     * Validate that required mappings exist for an analyzer
     * 
     * Required mappings: Sample ID, Test Code, Result Value
     * 
     * @param analyzerId The analyzer ID
     * @throws LIMSRuntimeException if required mappings are missing
     */
    void validateRequiredMappings(String analyzerId);

    /**
     * Activate a draft mapping
     * 
     * @param mappingId The mapping ID to activate
     * @param confirmed Whether user confirmed activation (required for active
     *                  analyzers)
     * @return The activated mapping
     * @throws LIMSRuntimeException if confirmation required but not provided
     */
    AnalyzerFieldMapping activateMapping(String mappingId, boolean confirmed);

    /**
     * Get all mappings for a specific analyzer field
     * 
     * @param analyzerFieldId The analyzer field ID
     * @return List of field mappings
     */
    List<AnalyzerFieldMapping> getMappingsByAnalyzerFieldId(String analyzerFieldId);

    /**
     * Get all mappings for an analyzer with complete data compiled Eagerly fetches
     * all relationships within transaction
     * 
     * @param analyzerId The analyzer ID
     * @return List of maps containing complete mapping data (field name, type,
     *         etc.)
     */
    List<Map<String, Object>> getMappingsForAnalyzer(String analyzerId);

    /**
     * Get all mappings for an analyzer with complete data compiled (optionally
     * including retired mappings)
     * 
     * 
     * @param analyzerId     The analyzer ID
     * @param includeRetired Whether to include retired (inactive) mappings
     * @return List of maps containing complete mapping data
     */
    List<Map<String, Object>> getMappingsForAnalyzer(String analyzerId, boolean includeRetired);

    /**
     * Create a mapping for an analyzer with validation Verifies analyzer field
     * belongs to analyzer, validates type compatibility
     * 
     * @param analyzerId             The analyzer ID
     * @param analyzerFieldId        The analyzer field ID
     * @param openelisFieldId        The OpenELIS field ID
     * @param openelisFieldType      The OpenELIS field type
     * @param mappingType            The mapping type
     * @param isRequired             Whether this is a required mapping
     * @param isActive               Whether mapping is active
     * @param specimenTypeConstraint Optional specimen type constraint
     * @param panelConstraint        Optional panel constraint
     * @return Map containing complete mapping data
     * @throws LIMSRuntimeException if validation fails
     */
    Map<String, Object> createMappingForAnalyzer(String analyzerId, String analyzerFieldId, String openelisFieldId,
            AnalyzerFieldMapping.OpenELISFieldType openelisFieldType, AnalyzerFieldMapping.MappingType mappingType,
            Boolean isRequired, Boolean isActive, String specimenTypeConstraint, String panelConstraint);

    /**
     * Get mapping with complete data compiled (for single mapping retrieval)
     * Eagerly fetches all relationships within transaction
     * 
     * @param mappingId The mapping ID
     * @return Map containing complete mapping data
     */
    Map<String, Object> getMappingWithCompleteData(String mappingId);

    /**
     * Verify mapping belongs to analyzer (within transaction)
     * 
     * @param mappingId  The mapping ID
     * @param analyzerId The analyzer ID to verify against
     * @return true if mapping belongs to analyzer, false otherwise
     */
    boolean verifyMappingBelongsToAnalyzer(String mappingId, String analyzerId);

    /**
     * Update an existing mapping with draft/active workflow support
     * 
     * For active analyzers, confirmation is required to update active mappings.
     * Draft mappings can be updated without confirmation.
     * 
     * @param mapping   The updated mapping
     * @param confirmed Whether user confirmed the update (required for active
     *                  analyzers)
     * @return The updated mapping
     * @throws LIMSRuntimeException if confirmation required but not provided, or
     *                              validation fails
     */
    AnalyzerFieldMapping updateMapping(AnalyzerFieldMapping mapping, boolean confirmed);

    /**
     * Disable/retire a mapping while preserving historical data
     * 
     * Sets is_active=false and logs retirement reason for audit trail. Cannot
     * disable required mappings (Sample ID, Test Code, Result Value).
     * 
     * @param mappingId        The mapping ID to disable
     * @param retirementReason Reason for retirement (for audit trail)
     * @return The disabled mapping
     * @throws LIMSRuntimeException if mapping is required or not found
     */
    AnalyzerFieldMapping disableMapping(String mappingId, String retirementReason);

    /**
     * Activate multiple mappings for an analyzer within a single transaction.
     *
     * <p>
     * All-or-nothing: if any mapping fails ownership verification or activation,
     * the entire batch rolls back.
     *
     * @param analyzerId The analyzer ID that owns the mappings
     * @param mappingIds List of mapping IDs to activate
     * @param confirmed  Whether user confirmed activation (required for active
     *                   analyzers)
     * @return The number of mappings activated
     * @throws LIMSRuntimeException if any mapping does not belong to the analyzer,
     *                              or if activation fails for any mapping
     */
    int bulkActivateMappings(String analyzerId, List<String> mappingIds, boolean confirmed);

    /**
     * Validate activation requirements for an analyzer
     * 
     * Performs comprehensive validation checks before allowing mapping activation:
     * - Required mappings present (Sample ID, Test Code, Result Value) - Pending
     * messages in error queue count - Concurrent edits detection (lastUpdated
     * check) - All active mappings have compatible types - Analyzer connection
     * operational (optional warning)
     * 
     * 
     * @param analyzerId The analyzer ID to validate
     * @return ActivationValidationResult containing validation status, missing
     *         required fields, pending message count, and warnings
     */
    ActivationValidationResult validateActivation(String analyzerId);
}
