package org.openelisglobal.analyzerqc.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzerqc.valueholder.AnalyzerQcRun;
import org.openelisglobal.common.dao.BaseDAO;

/**
 * Persistence operations for AnalyzerQcRun.
 *
 * Extends BaseDAO following the same pattern as AnalyzerQcRuleDAO,
 * with additional query methods needed for QC status evaluation.
 */
public interface AnalyzerQcRunDAO extends BaseDAO<AnalyzerQcRun, String> {

    /**
     * Returns the most recent PASS run for an analyzer.
     * Used by status evaluation to determine if QC is still valid.
     */
    Optional<AnalyzerQcRun> getLastPassForAnalyzer(String analyzerId);

    /**
     * Returns the most recent run of any result for an analyzer.
     * Used to display "last run" info in the UI status panel.
     */
    Optional<AnalyzerQcRun> getLastRunForAnalyzer(String analyzerId);

    /**
     * Returns all runs for an analyzer, most recent first.
     * Used by the history endpoint.
     */
    List<AnalyzerQcRun> getAllRunsForAnalyzer(String analyzerId);
}
