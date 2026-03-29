package org.openelisglobal.analyzer.service;

import java.util.List;

/**
 * Service interface for applying field mappings to ASTM message segments
 * 
 * 
 * This service transforms raw ASTM message segments by applying configured
 * field mappings. Used by MappingAwareAnalyzerLineInserter wrapper to apply
 * mappings before delegating to plugin inserter.
 */
public interface MappingApplicationService {

    /**
     * Apply mappings to raw ASTM message segments
     * 
     * 
     * Receives raw ASTM message segments (List<String> lines), extracts test codes,
     * units, and qualitative values, queries AnalyzerFieldMapping, applies
     * mappings, and returns transformed data structure.
     * 
     * @param analyzerId The analyzer ID
     * @param lines      Raw ASTM message segments (List<String>)
     * @return MappingApplicationResult containing: - transformedLines: Transformed
     *         ASTM lines with mapped values (if mappings found) - unmappedFields:
     *         List of fields that could not be mapped - errors: List of errors
     *         encountered during mapping - hasMappings: Whether any mappings were
     *         found and applied
     */
    MappingApplicationResult applyMappings(String analyzerId, List<String> lines);

    /**
     * Check if analyzer has active mappings configured
     * 
     * @param analyzerId The analyzer ID
     * @return true if analyzer has active mappings, false otherwise
     */
    boolean hasActiveMappings(String analyzerId);
}
