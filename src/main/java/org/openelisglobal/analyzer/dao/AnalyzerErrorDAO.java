package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerErrorDAO extends BaseDAO<AnalyzerError, String> {
    List<AnalyzerError> findByAnalyzerId(String analyzerId);

    /**
     * Set analyzer_id = NULL on all error rows for a given analyzer. Used during
     * analyzer deletion to preserve audit trail (SET NULL tier).
     *
     * @param analyzerId The analyzer ID
     * @return Number of rows updated
     */
    int nullifyAnalyzerId(String analyzerId);

    List<AnalyzerError> findByStatus(AnalyzerError.ErrorStatus status);

    List<AnalyzerError> findByErrorType(AnalyzerError.ErrorType errorType);

    List<AnalyzerError> findBySeverity(AnalyzerError.Severity severity);

    List<AnalyzerError> findAll();

    /**
     * Get AnalyzerError by ID with analyzer eagerly fetched
     *
     * @param errorId Error ID
     * @return AnalyzerError with analyzer relationship loaded, or null if not found
     */
    java.util.Optional<AnalyzerError> getWithAnalyzer(String errorId);

    /**
     * Find errors matching all non-null filter criteria. All parameters are
     * optional; passing all nulls returns all errors.
     *
     * @param analyzerId Optional analyzer ID
     * @param errorType  Optional error type
     * @param severity   Optional severity
     * @param status     Optional status
     * @param startDate  Optional start date (inclusive)
     * @param endDate    Optional end date (inclusive)
     * @return Matching errors ordered by lastupdated DESC, with analyzer eagerly
     *         fetched
     */
    List<AnalyzerError> findByFilters(String analyzerId, AnalyzerError.ErrorType errorType,
            AnalyzerError.Severity severity, AnalyzerError.ErrorStatus status, java.util.Date startDate,
            java.util.Date endDate);

    /**
     * Return global error statistics independent of any search filters.
     *
     * @return Map with keys: totalErrors, unacknowledged, critical, last24Hours
     *         (all Long values)
     */
    java.util.Map<String, Long> getGlobalStatistics();
}
