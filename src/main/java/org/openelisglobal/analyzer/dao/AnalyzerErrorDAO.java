package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerErrorDAO extends BaseDAO<AnalyzerError, String> {
    List<AnalyzerError> findByAnalyzerId(String analyzerId);

    List<AnalyzerError> findByStatus(String status);

    List<AnalyzerError> findByErrorType(String errorType);

    List<AnalyzerError> findBySeverity(String severity);

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
