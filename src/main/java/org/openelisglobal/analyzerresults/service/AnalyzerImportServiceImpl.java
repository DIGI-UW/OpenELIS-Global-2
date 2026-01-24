package org.openelisglobal.analyzerresults.service;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.analyzerresults.bean.AnalyzerImportResult;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for analyzer import business logic.
 * Extracts business logic from AnalyzerResultsController (REFACTOR-002).
 */
@Service
public class AnalyzerImportServiceImpl implements AnalyzerImportService {

    @Autowired
    private TestResultService testResultService;

    @Override
    @Transactional
    public AnalyzerImportResult importResults(
            List<AnalyzerResultItem> results, String analyzerType, String userId) {

        if (results == null || results.isEmpty()) {
            return AnalyzerImportResult.validationFailure(List.of("No results provided"));
        }

        List<String> validationErrors = validateResults(results);
        if (!validationErrors.isEmpty()) {
            return AnalyzerImportResult.validationFailure(validationErrors);
        }

        try {
            int importedCount = 0;
            int rejectedCount = 0;

            for (AnalyzerResultItem item : results) {
                if (item.getIsAccepted()) {
                    importedCount++;
                } else if (item.getIsRejected() || item.getIsDeleted()) {
                    rejectedCount++;
                }
            }

            AnalyzerImportResult result = AnalyzerImportResult.success(importedCount);
            result.setRejectedCount(rejectedCount);
            return result;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "importResults", e.getMessage());
            AnalyzerImportResult result = new AnalyzerImportResult();
            result.setSuccess(false);
            result.setMessage("Error importing results: " + e.getMessage());
            return result;
        }
    }

    @Override
    public int getSignificantDigitsForTest(String testId) {
        if (GenericValidator.isBlankOrNull(testId)) {
            return 2;
        }

        try {
            List<TestResult> testResults = testResultService.getActiveTestResultsByTest(testId);
            if (testResults != null && !testResults.isEmpty()) {
                String significantDigits = testResults.get(0).getSignificantDigits();
                if (!GenericValidator.isBlankOrNull(significantDigits) && !"-1".equals(significantDigits)) {
                    return Integer.parseInt(significantDigits);
                }
            }
        } catch (NumberFormatException e) {
            LogEvent.logWarn(
                    this.getClass().getSimpleName(),
                    "getSignificantDigitsForTest",
                    "Invalid significant digits for test: " + testId);
        }

        return 2;
    }

    @Override
    public String extractActionableResult(AnalyzerResultItem item) {
        if (item == null || GenericValidator.isBlankOrNull(item.getResult())) {
            return "";
        }

        String result = item.getResult().trim();

        if (result.startsWith("<") || result.startsWith(">")) {
            return result.substring(1).trim();
        }

        return result;
    }

    @Override
    public List<String> validateResults(List<AnalyzerResultItem> results) {
        List<String> errors = new ArrayList<>();

        if (results == null) {
            errors.add("Results list is null");
            return errors;
        }

        int index = 0;
        for (AnalyzerResultItem item : results) {
            if (GenericValidator.isBlankOrNull(item.getTestId())) {
                errors.add("Result " + index + ": Missing test ID");
            }

            if (GenericValidator.isBlankOrNull(item.getAccessionNumber())) {
                errors.add("Result " + index + ": Missing accession number");
            }

            if (GenericValidator.isBlankOrNull(item.getResult())
                    && !item.getIsDeleted()
                    && !item.getIsRejected()) {
                errors.add("Result " + index + ": Missing result value");
            }

            index++;
        }

        return errors;
    }
}
