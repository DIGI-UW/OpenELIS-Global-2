package org.openelisglobal.analyzer.service;

import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * Service interface for copying analyzer field mappings
 * 
 * 
 * Provides methods for copying field mappings from one analyzer to another
 * with: - Conflict resolution (overwrite, merge) - Type compatibility
 * validation - Transaction rollback on failure
 */
public interface AnalyzerMappingCopyService {

    /**
     * Copy all field mappings from source analyzer to target analyzer
     * 
     * 
     * @param sourceAnalyzerId The source analyzer ID
     * @param targetAnalyzerId The target analyzer ID
     * @param options          Copy options (overwrite existing, skip incompatible,
     *                         etc.)
     * @return CopyMappingsResult containing copied count, skipped count, warnings,
     *         and conflicts
     * @throws LIMSRuntimeException if validation fails or operation cannot proceed
     */
    CopyMappingsResult copyMappings(String sourceAnalyzerId, String targetAnalyzerId, CopyOptions options);
}
