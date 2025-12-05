package org.openelisglobal.typeofsample.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handler for loading test-sample type configuration files. Supports CSV format
 * for mapping tests to their valid sample types.
 *
 * Expected CSV format: testName,sampleType Complete Blood Count,Whole Blood
 * Hemoglobin,Whole Blood Hemoglobin,Serum Glucose,Serum Glucose,Plasma
 *
 * Notes: - First line is the header (required) - testName and sampleType are
 * required fields - testName can be the localized test name or description -
 * sampleType is the localized name or description of the sample type - Multiple
 * rows can have the same testName with different sampleTypes - Existing
 * mappings are preserved, new mappings are added
 */
@Component
public class TestSampleTypeConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private TestService testService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;

    @Override
    public String getDomainName() {
        return "test-sample-types";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 210; // Depends on tests and sample types
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Read and validate header
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Test-sample type configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        validateHeaders(headers, fileName);

        // Get column indices
        int testNameIndex = findColumnIndex(headers, "testName");
        int sampleTypeIndex = findColumnIndex(headers, "sampleType");

        List<TypeOfSampleTest> processedMappings = new ArrayList<>();
        String line;
        int lineNumber = 1; // Start at 1 since we already read the header

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            // Skip empty lines
            if (line.trim().isEmpty()) {
                continue;
            }

            try {
                String[] values = parseCsvLine(line);
                TypeOfSampleTest mapping = processCsvLine(values, testNameIndex, sampleTypeIndex, lineNumber, fileName);
                if (mapping != null) {
                    processedMappings.add(mapping);
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }

        // Refresh display lists to reflect new mappings
        DisplayListService.getInstance().refreshLists();

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Successfully loaded " + processedMappings.size() + " test-sample type mappings from " + fileName);
    }

    private String[] parseCsvLine(String line) {
        // Simple CSV parser that handles quoted fields
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
        boolean hasSampleTypeColumn = false;

        for (String header : headers) {
            if ("testName".equalsIgnoreCase(header)) {
                hasTestNameColumn = true;
            }
            if ("sampleType".equalsIgnoreCase(header)) {
                hasSampleTypeColumn = true;
            }
        }

        if (!hasTestNameColumn) {
            throw new IllegalArgumentException(
                    "Test-sample type configuration file " + fileName + " must have a 'testName' column");
        }
        if (!hasSampleTypeColumn) {
            throw new IllegalArgumentException(
                    "Test-sample type configuration file " + fileName + " must have a 'sampleType' column");
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

    private TypeOfSampleTest processCsvLine(String[] values, int testNameIndex, int sampleTypeIndex, int lineNumber,
            String fileName) {

        // Get required fields
        String testName = getValueOrEmpty(values, testNameIndex);
        String sampleTypeName = getValueOrEmpty(values, sampleTypeIndex);

        if (testName.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Skipping row " + lineNumber + " in " + fileName + " with missing testName");
            return null;
        }

        if (sampleTypeName.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Skipping row " + lineNumber + " in " + fileName + " with missing sampleType");
            return null;
        }

        // Find test by name
        Test test = findTestByName(testName);
        if (test == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Test '" + testName + "' not found in line " + lineNumber + " of " + fileName + ". Skipping.");
            return null;
        }

        // Find sample type by name
        TypeOfSample sampleType = findSampleTypeByName(sampleTypeName);
        if (sampleType == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine", "Sample type '" + sampleTypeName
                    + "' not found in line " + lineNumber + " of " + fileName + ". Skipping.");
            return null;
        }

        // Check if mapping already exists
        if (mappingExists(test.getId(), sampleType.getId())) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "processCsvLine", "Mapping already exists for test '"
                    + testName + "' and sample type '" + sampleTypeName + "'. Skipping.");
            return null;
        }

        // Create new mapping
        TypeOfSampleTest mapping = new TypeOfSampleTest();
        mapping.setTestId(test.getId());
        mapping.setTypeOfSampleId(sampleType.getId());
        mapping.setSysUserId("1"); // System user for configuration loading

        String mappingId = typeOfSampleTestService.insert(mapping);
        mapping.setId(mappingId);

        LogEvent.logInfo(this.getClass().getSimpleName(), "processCsvLine",
                "Created mapping: test '" + testName + "' -> sample type '" + sampleTypeName + "'");

        return mapping;
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value : "";
        }
        return "";
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

        // Try by name (which uses localized name internally)
        test = testService.getTestByName(testName);
        return test;
    }

    private TypeOfSample findSampleTypeByName(String sampleTypeName) {
        // Get all sample types and search by localized name or description
        List<TypeOfSample> allSampleTypes = typeOfSampleService.getAllTypeOfSamples();

        for (TypeOfSample sampleType : allSampleTypes) {
            // Check localized name
            if (sampleType.getLocalizedName() != null
                    && sampleType.getLocalizedName().equalsIgnoreCase(sampleTypeName)) {
                return sampleType;
            }
            // Check description
            if (sampleType.getDescription() != null && sampleType.getDescription().equalsIgnoreCase(sampleTypeName)) {
                return sampleType;
            }
            // Check local abbreviation
            if (sampleType.getLocalAbbreviation() != null
                    && sampleType.getLocalAbbreviation().equalsIgnoreCase(sampleTypeName)) {
                return sampleType;
            }
        }

        return null;
    }

    private boolean mappingExists(String testId, String sampleTypeId) {
        List<TypeOfSampleTest> existingMappings = typeOfSampleTestService.getTypeOfSampleTestsForTest(testId);
        for (TypeOfSampleTest mapping : existingMappings) {
            if (mapping.getTypeOfSampleId().equals(sampleTypeId)) {
                return true;
            }
        }
        return false;
    }
}
