package org.openelisglobal.testresult.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handler for loading test result configuration files. Supports CSV format for
 * defining test results (possible values/options for tests).
 *
 * Expected CSV format:
 * testName,resultType,resultValue,sortOrder,isQuantifiable,isActive,isNormal,significantDigits,flags
 * HIV Rapid Test,D,Positive,1,N,Y,N,, HIV Rapid Test,D,Negative,2,N,Y,Y,, HIV
 * Rapid Test,D,Inconclusive,3,N,Y,N,, Hemoglobin,N,,1,Y,Y,N,2,
 * Glucose,N,,1,Y,Y,N,0,
 *
 * Result Types: - D = Dictionary (coded value from dictionary entry) - N =
 * Numeric (numeric result with optional significant digits) - A = Alpha
 * (alphanumeric text) - R = Remark (free-form text) - T = Titer (titer ranges
 * like 1:10, 1:20) - M = Multiselect (multiple dictionary values) - C =
 * Cascading multiselect
 *
 * Notes: - First line is the header (required) - testName and resultType are
 * required fields - resultValue is required for Dictionary (D) type, optional
 * for others - For Dictionary types, resultValue should match an existing
 * dictionary entry - sortOrder is optional (auto-assigned if not provided) -
 * isQuantifiable defaults to "N" if not specified - isActive defaults to "Y" if
 * not specified - isNormal defaults to "N" if not specified - significantDigits
 * is optional, used for numeric types - flags is optional (e.g., "H" for high,
 * "L" for low) - Existing test results with matching test and value will be
 * updated
 */
@Component
public class TestResultConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private TestService testService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private DictionaryService dictionaryService;

    @Override
    public String getDomainName() {
        return "test-results";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 310; // After tests (200) and dictionaries (300)
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Read and validate header
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Test result configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        validateHeaders(headers, fileName);

        // Get column indices
        int testNameIndex = findColumnIndex(headers, "testName");
        int resultTypeIndex = findColumnIndex(headers, "resultType");
        int resultValueIndex = findColumnIndex(headers, "resultValue");
        int dictionaryCategoryIndex = findColumnIndex(headers, "dictionaryCategory");
        int sortOrderIndex = findColumnIndex(headers, "sortOrder");
        int isQuantifiableIndex = findColumnIndex(headers, "isQuantifiable");
        int isActiveIndex = findColumnIndex(headers, "isActive");
        int isNormalIndex = findColumnIndex(headers, "isNormal");
        int significantDigitsIndex = findColumnIndex(headers, "significantDigits");
        int flagsIndex = findColumnIndex(headers, "flags");

        List<TestResult> processedResults = new ArrayList<>();
        String line;
        int lineNumber = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            // Skip empty lines and comments (lines starting with #)
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            try {
                String[] values = parseCsvLine(line);
                TestResult testResult = processCsvLine(values, testNameIndex, resultTypeIndex, resultValueIndex,
                        dictionaryCategoryIndex, sortOrderIndex, isQuantifiableIndex, isActiveIndex, isNormalIndex,
                        significantDigitsIndex, flagsIndex, lineNumber, fileName);
                if (testResult != null) {
                    processedResults.add(testResult);
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }

        // Refresh display lists
        DisplayListService.getInstance().refreshLists();

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Successfully loaded " + processedResults.size() + " test results from " + fileName);
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        values.add(currentValue.toString().trim());

        return values.toArray(new String[0]);
    }

    private void validateHeaders(String[] headers, String fileName) {
        boolean hasTestNameColumn = false;
        boolean hasResultTypeColumn = false;

        for (String header : headers) {
            if ("testName".equalsIgnoreCase(header)) {
                hasTestNameColumn = true;
            }
            if ("resultType".equalsIgnoreCase(header)) {
                hasResultTypeColumn = true;
            }
        }

        if (!hasTestNameColumn) {
            throw new IllegalArgumentException(
                    "Test result configuration file " + fileName + " must have a 'testName' column");
        }
        if (!hasResultTypeColumn) {
            throw new IllegalArgumentException(
                    "Test result configuration file " + fileName + " must have a 'resultType' column");
        }
    }

    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (columnName.equalsIgnoreCase(headers[i])) {
                return i;
            }
        }
        return -1;
    }

    private TestResult processCsvLine(String[] values, int testNameIndex, int resultTypeIndex, int resultValueIndex,
            int dictionaryCategoryIndex, int sortOrderIndex, int isQuantifiableIndex, int isActiveIndex,
            int isNormalIndex, int significantDigitsIndex, int flagsIndex, int lineNumber, String fileName) {

        String testName = getValueOrEmpty(values, testNameIndex);
        String resultType = getValueOrEmpty(values, resultTypeIndex);

        if (testName.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Skipping row " + lineNumber + " in " + fileName + " with missing testName");
            return null;
        }

        if (resultType.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Skipping row " + lineNumber + " in " + fileName + " with missing resultType");
            return null;
        }

        // Validate result type
        resultType = resultType.toUpperCase();
        if (!isValidResultType(resultType)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine", "Invalid resultType '" + resultType
                    + "' in line " + lineNumber + " of " + fileName + ". Valid types: D, N, A, R, T, M, C. Skipping.");
            return null;
        }

        // Find test by name
        Test test = findTestByName(testName);
        if (test == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Test '" + testName + "' not found in line " + lineNumber + " of " + fileName + ". Skipping.");
            return null;
        }

        String resultValue = getValueOrEmpty(values, resultValueIndex);
        String dictionaryCategory = getValueOrEmpty(values, dictionaryCategoryIndex);

        // For Dictionary types, resultValue is required and must be a valid dictionary
        // entry
        if ("D".equals(resultType) || "M".equals(resultType) || "C".equals(resultType)) {
            if (resultValue.isEmpty()) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                        "Dictionary result type requires resultValue in line " + lineNumber + " of " + fileName
                                + ". Skipping.");
                return null;
            }

            // Find dictionary entry, optionally by category
            Dictionary dictionary = null;
            if (!dictionaryCategory.isEmpty()) {
                // Look up by entry name and category
                dictionary = dictionaryService.getDictionaryEntrysByNameAndCategoryDescription(resultValue,
                        dictionaryCategory);
            }
            if (dictionary == null) {
                // Fall back to lookup by entry name only
                dictionary = dictionaryService.getDictionaryByDictEntry(resultValue);
            }
            if (dictionary == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                        "Dictionary entry '" + resultValue + "'"
                                + (dictionaryCategory.isEmpty() ? "" : " in category '" + dictionaryCategory + "'")
                                + " not found in line " + lineNumber + " of " + fileName + ". Skipping.");
                return null;
            }

            // For dictionary types, store the dictionary ID as the value
            resultValue = dictionary.getId();
        }

        // Check if test result already exists
        TestResult existingResult = findExistingTestResult(test.getId(), resultType, resultValue);

        TestResult testResult;
        if (existingResult != null) {
            testResult = updateTestResult(existingResult, values, sortOrderIndex, isQuantifiableIndex, isActiveIndex,
                    isNormalIndex, significantDigitsIndex, flagsIndex);
            LogEvent.logInfo(this.getClass().getSimpleName(), "processCsvLine",
                    "Updated existing test result for test: " + testName);
        } else {
            testResult = createTestResult(test, resultType, resultValue, values, sortOrderIndex, isQuantifiableIndex,
                    isActiveIndex, isNormalIndex, significantDigitsIndex, flagsIndex);
            LogEvent.logInfo(this.getClass().getSimpleName(), "processCsvLine",
                    "Created new test result for test: " + testName);
        }

        return testResult;
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value : "";
        }
        return "";
    }

    private boolean isValidResultType(String resultType) {
        return "D".equals(resultType) || "N".equals(resultType) || "A".equals(resultType) || "R".equals(resultType)
                || "T".equals(resultType) || "M".equals(resultType) || "C".equals(resultType);
    }

    private Test findTestByName(String testName) {
        // Try to find test by localized name first
        Test test = testService.getTestByLocalizedName(testName);
        if (test != null) {
            return test;
        }

        // Try by description
        test = testService.getTestByDescription(testName);
        if (test != null) {
            return test;
        }

        // Try by name
        test = testService.getTestByName(testName);
        return test;
    }

    private TestResult findExistingTestResult(String testId, String resultType, String resultValue) {
        List<TestResult> existingResults = testResultService.getActiveTestResultsByTest(testId);

        for (TestResult result : existingResults) {
            // Match by type and value
            if (resultType.equals(result.getTestResultType())) {
                // For dictionary types and titer, match by value
                // D, M, C store dictionary IDs; T stores titer values like "1:10"
                if ("D".equals(resultType) || "M".equals(resultType) || "C".equals(resultType)
                        || "T".equals(resultType)) {
                    if (resultValue != null && resultValue.equals(result.getValue())) {
                        return result;
                    }
                } else {
                    // For N, A, R types, there's typically one result per type
                    return result;
                }
            }
        }

        return null;
    }

    private TestResult updateTestResult(TestResult testResult, String[] values, int sortOrderIndex,
            int isQuantifiableIndex, int isActiveIndex, int isNormalIndex, int significantDigitsIndex, int flagsIndex) {

        // Update sort order
        String sortOrder = getValueOrEmpty(values, sortOrderIndex);
        if (!sortOrder.isEmpty()) {
            testResult.setSortOrder(sortOrder);
        }

        // Update isQuantifiable
        String isQuantifiable = getValueOrEmpty(values, isQuantifiableIndex);
        if (!isQuantifiable.isEmpty()) {
            testResult
                    .setIsQuantifiable("Y".equalsIgnoreCase(isQuantifiable) || "true".equalsIgnoreCase(isQuantifiable));
        }

        // Update isActive
        String isActive = getValueOrEmpty(values, isActiveIndex);
        if (!isActive.isEmpty()) {
            testResult.setIsActive("Y".equalsIgnoreCase(isActive) || "true".equalsIgnoreCase(isActive));
        }

        // Update isNormal
        String isNormal = getValueOrEmpty(values, isNormalIndex);
        if (!isNormal.isEmpty()) {
            testResult.setIsNormal("Y".equalsIgnoreCase(isNormal) || "true".equalsIgnoreCase(isNormal));
        }

        // Update significant digits
        String significantDigits = getValueOrEmpty(values, significantDigitsIndex);
        if (!significantDigits.isEmpty()) {
            testResult.setSignificantDigits(significantDigits);
        }

        // Update flags
        String flags = getValueOrEmpty(values, flagsIndex);
        if (!flags.isEmpty()) {
            testResult.setFlags(flags);
        }

        testResult.setSysUserId("1");
        testResultService.update(testResult);
        return testResult;
    }

    private TestResult createTestResult(Test test, String resultType, String resultValue, String[] values,
            int sortOrderIndex, int isQuantifiableIndex, int isActiveIndex, int isNormalIndex,
            int significantDigitsIndex, int flagsIndex) {

        TestResult testResult = new TestResult();
        testResult.setTest(test);
        testResult.setTestResultType(resultType);
        testResult.setValue(resultValue);

        // Set sort order
        String sortOrder = getValueOrEmpty(values, sortOrderIndex);
        if (!sortOrder.isEmpty()) {
            testResult.setSortOrder(sortOrder);
        } else {
            // Auto-assign sort order based on existing results count
            List<TestResult> existingResults = testResultService.getActiveTestResultsByTest(test.getId());
            testResult.setSortOrder(String.valueOf(existingResults.size() + 1));
        }

        // Set isQuantifiable
        String isQuantifiable = getValueOrEmpty(values, isQuantifiableIndex);
        if (!isQuantifiable.isEmpty()) {
            testResult
                    .setIsQuantifiable("Y".equalsIgnoreCase(isQuantifiable) || "true".equalsIgnoreCase(isQuantifiable));
        } else {
            testResult.setIsQuantifiable(false);
        }

        // Set isActive
        String isActive = getValueOrEmpty(values, isActiveIndex);
        if (!isActive.isEmpty()) {
            testResult.setIsActive("Y".equalsIgnoreCase(isActive) || "true".equalsIgnoreCase(isActive));
        } else {
            testResult.setIsActive(true);
        }

        // Set isNormal
        String isNormal = getValueOrEmpty(values, isNormalIndex);
        if (!isNormal.isEmpty()) {
            testResult.setIsNormal("Y".equalsIgnoreCase(isNormal) || "true".equalsIgnoreCase(isNormal));
        } else {
            testResult.setIsNormal(false);
        }

        // Set significant digits
        String significantDigits = getValueOrEmpty(values, significantDigitsIndex);
        if (!significantDigits.isEmpty()) {
            testResult.setSignificantDigits(significantDigits);
        }

        // Set flags
        String flags = getValueOrEmpty(values, flagsIndex);
        if (!flags.isEmpty()) {
            testResult.setFlags(flags);
        }

        testResult.setSysUserId("1");
        String testResultId = testResultService.insert(testResult);
        testResult.setId(testResultId);

        return testResult;
    }
}
