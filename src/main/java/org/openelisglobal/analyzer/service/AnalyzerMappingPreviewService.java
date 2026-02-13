package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;

/**
 * Service interface for analyzer mapping preview operations
 * 
 * 
 * Provides stateless preview operations for testing field mappings with sample
 * ASTM messages
 */
public interface AnalyzerMappingPreviewService {

    /**
     * Preview how a sample ASTM message will be interpreted with current mappings
     * 
     * 
     * @param analyzerId  The analyzer ID
     * @param astmMessage The sample ASTM message (max 10KB)
     * @param options     Preview options (detailed parsing, validation)
     * @return MappingPreviewResult containing parsed fields, applied mappings,
     *         entity preview, warnings, and errors
     */
    MappingPreviewResult previewMapping(String analyzerId, String astmMessage, PreviewOptions options);

    /**
     * Parse ASTM message into structured fields
     * 
     * @param astmMessage The ASTM message to parse
     * @return List of parsed fields
     */
    List<ParsedField> parseAstmMessage(String astmMessage);

    /**
     * Apply mappings to parsed fields
     * 
     * @param parsedFields The parsed fields from ASTM message
     * @param mappings     The active mappings for the analyzer
     * @return List of applied mappings
     */
    List<AppliedMapping> applyMappings(List<ParsedField> parsedFields, List<AnalyzerFieldMapping> mappings);

    /**
     * Build entity preview from applied mappings
     * 
     * @param appliedMappings The applied mappings
     * @return EntityPreview containing Test, Result, and Sample entities
     */
    EntityPreview buildEntityPreview(List<AppliedMapping> appliedMappings);
}
