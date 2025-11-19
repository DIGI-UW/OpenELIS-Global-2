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
}
