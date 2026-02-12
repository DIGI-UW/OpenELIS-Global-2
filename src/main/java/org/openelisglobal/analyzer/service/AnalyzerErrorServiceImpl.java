package org.openelisglobal.analyzer.service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import org.openelisglobal.analyzer.dao.AnalyzerErrorDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for AnalyzerError operations
 * 
 * 
 * Provides business logic for: - Creating error records for unmapped/failed
 * analyzer messages - Acknowledging errors - Reprocessing errors after mappings
 * are created - Querying errors with filters
 */
@Service
@Transactional
public class AnalyzerErrorServiceImpl implements AnalyzerErrorService {

    @Autowired
    private AnalyzerErrorDAO analyzerErrorDAO;

    @Autowired(required = false)
    private AnalyzerReprocessingService analyzerReprocessingService;

    @Override
    @Transactional
    public String createError(Analyzer analyzer, AnalyzerError.ErrorType errorType, AnalyzerError.Severity severity,
            String errorMessage, String rawMessage) {
        AnalyzerError error = new AnalyzerError();
        error.setAnalyzer(analyzer);
        error.setErrorType(errorType);
        error.setSeverity(severity);
        error.setErrorMessage(errorMessage);
        error.setRawMessage(rawMessage);
        error.setStatus(AnalyzerError.ErrorStatus.UNACKNOWLEDGED);
        error.setLastupdatedFields();

        return analyzerErrorDAO.insert(error);
    }

    @Override
    @Transactional
    public void acknowledgeError(String errorId, String userId) {
        AnalyzerError error = analyzerErrorDAO.get(errorId)
                .orElseThrow(() -> new LIMSRuntimeException("AnalyzerError not found: " + errorId));

        error.setStatus(AnalyzerError.ErrorStatus.ACKNOWLEDGED);
        error.setAcknowledgedBy(userId);
        error.setAcknowledgedAt(new Timestamp(System.currentTimeMillis()));
        error.setLastupdatedFields();
        error.setSysUserId(userId);

        analyzerErrorDAO.update(error);
    }

    @Override
    @Transactional
    public boolean reprocessError(String errorId) {
        AnalyzerError error = analyzerErrorDAO.get(errorId)
                .orElseThrow(() -> new LIMSRuntimeException("AnalyzerError not found: " + errorId));

        if (analyzerReprocessingService == null) {
            throw new LIMSRuntimeException("AnalyzerReprocessingService not available");
        }

        boolean success = analyzerReprocessingService.reprocessMessage(error);

        if (success) {
            error.setStatus(AnalyzerError.ErrorStatus.RESOLVED);
            error.setResolvedAt(new Timestamp(System.currentTimeMillis()));
            error.setLastupdatedFields();
            analyzerErrorDAO.update(error);
        }

        return success;
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyzerError getErrorById(String errorId) {
        // Use getWithAnalyzer to eagerly fetch analyzer relationship
        // This prevents LazyInitializationException when controller accesses
        // analyzer.name
        return analyzerErrorDAO.getWithAnalyzer(errorId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerError> getErrorsByFilters(String analyzerId, AnalyzerError.ErrorType errorType,
            AnalyzerError.Severity severity, AnalyzerError.ErrorStatus status, Date startDate, Date endDate) {
        return analyzerErrorDAO.findByFilters(analyzerId, errorType, severity, status, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> getErrorStatistics() {
        return analyzerErrorDAO.getGlobalStatistics();
    }
}
