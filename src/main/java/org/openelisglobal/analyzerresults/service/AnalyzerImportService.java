package org.openelisglobal.analyzerresults.service;

import java.util.List;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.analyzerresults.bean.AnalyzerImportResult;

/**
 * Service interface for analyzer import business logic.
 * Extracts business logic from AnalyzerResultsController (REFACTOR-002).
 */
public interface AnalyzerImportService {

    /**
     * Import analyzer results with QC validation and enrichment.
     *
     * @param results      List of analyzer result items to import
     * @param analyzerType Type of analyzer
     * @param userId       User performing the import
     * @return AnalyzerImportResult with success status and details
     */
    AnalyzerImportResult importResults(List<AnalyzerResultItem> results, String analyzerType, String userId);

    /**
     * Get significant digits for a test result.
     *
     * @param testId The test ID
     * @return Number of significant digits
     */
    int getSignificantDigitsForTest(String testId);

    /**
     * Extract actionable result from analyzer result item.
     *
     * @param item The analyzer result item
     * @return Processed result value
     */
    String extractActionableResult(AnalyzerResultItem item);

    /**
     * Validate analyzer results before import.
     *
     * @param results List of results to validate
     * @return List of validation error messages (empty if valid)
     */
    List<String> validateResults(List<AnalyzerResultItem> results);
}
