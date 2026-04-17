package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service interface for AnalyzerError operations
 * 
 * 
 * Provides methods for: - Creating error records for unmapped/failed analyzer
 * messages - Acknowledging errors - Reprocessing errors after mappings are
 * created - Querying errors with filters
 */
public interface AnalyzerErrorService {

    /**
     * Create a new analyzer error record
     * 
     * @param analyzer     The analyzer that generated the error
     * @param errorType    The type of error (MAPPING, VALIDATION, etc.)
     * @param severity     The severity level (CRITICAL, ERROR, WARNING)
     * @param errorMessage Human-readable error message
     * @param rawMessage   Raw ASTM message (for reprocessing)
     * @return The ID of the created error record
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    String createError(Analyzer analyzer, AnalyzerError.ErrorType errorType, AnalyzerError.Severity severity,
            String errorMessage, String rawMessage);

    /**
     * Acknowledge an error (mark as acknowledged by user)
     * 
     * @param errorId The ID of the error to acknowledge
     * @param userId  The ID of the user acknowledging the error
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    void acknowledgeError(String errorId, String userId);

    /**
     * Reprocess an error message after mappings are created
     * 
     * @param errorId The ID of the error to reprocess
     * @return true if reprocessing succeeded, false otherwise
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    boolean reprocessError(String errorId);

    /**
     * Get error by ID
     * 
     * @param errorId The ID of the error to retrieve
     * @return The error, or null if not found
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerError getErrorById(String errorId);

    /**
     * Get errors filtered by various criteria. All non-null parameters are combined
     * (AND logic).
     *
     * @param analyzerId Optional analyzer ID filter
     * @param errorType  Optional error type filter
     * @param severity   Optional severity filter
     * @param status     Optional status filter
     * @param startDate  Optional start date filter
     * @param endDate    Optional end date filter
     * @return List of matching errors
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    List<AnalyzerError> getErrorsByFilters(String analyzerId, AnalyzerError.ErrorType errorType,
            AnalyzerError.Severity severity, AnalyzerError.ErrorStatus status, java.util.Date startDate,
            java.util.Date endDate);

    /**
     * Get global error statistics independent of any search/filter criteria.
     *
     * @return Map with keys: totalErrors, unacknowledged, critical, last24Hours
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    java.util.Map<String, Long> getErrorStatistics();
}
