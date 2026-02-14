package org.openelisglobal.test.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handler for loading test configuration files. Supports CSV format for
 * defining laboratory tests with their sample type mappings.
 *
 * Expected CSV format:
 * testName,testSection,sampleType,loinc,isActive,isOrderable,sortOrder,unitOfMeasure,englishName,frenchName
 * Glucose,Biochemistry,Serum,2345-7,Y,Y,1,mg/dL,Glucose,Glucose
 * Hemoglobin,Hematology,Whole Blood,718-7,Y,Y,2,g/dL,Hemoglobin,Hémoglobine HIV
 * Rapid Test,Serology,Whole Blood,68961-2,Y,Y,3,,HIV Rapid Test,Test Rapide VIH
 *
 * Notes: - First line is the header (required) - testName and testSection are
 * required fields - sampleType is optional but recommended (can specify
 * multiple separated by |) - loinc is optional but recommended for
 * interoperability - isActive defaults to "Y" if not specified - isOrderable
 * defaults to "Y" if not specified - sortOrder is optional (auto-assigned if
 * not provided) - unitOfMeasure is optional - englishName defaults to testName
 * if not provided - frenchName defaults to englishName if not provided -
 * Existing tests with matching description will be updated
 */
@Component
public class TestConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private TestService testService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;

    @Autowired
    private UnitOfMeasureService unitOfMeasureService;

    @Override
    public String getDomainName() {
        return "tests";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 200; // Depends on test sections and sample types
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Read and validate header
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Test configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        validateHeaders(headers, fileName);

        // Get column indices
        int testNameIndex = findColumnIndex(headers, "testName");
        int testSectionIndex = findColumnIndex(headers, "testSection");
        int sampleTypeIndex = findColumnIndex(headers, "sampleType");
        int loincIndex = findColumnIndex(headers, "loinc");
        int isActiveIndex = findColumnIndex(headers, "isActive");
        int isOrderableIndex = findColumnIndex(headers, "isOrderable");
        int sortOrderIndex = findColumnIndex(headers, "sortOrder");
        int unitOfMeasureIndex = findColumnIndex(headers, "unitOfMeasure");
        int englishNameIndex = findColumnIndex(headers, "englishName");
        int frenchNameIndex = findColumnIndex(headers, "frenchName");

        List<Test> processedTests = new ArrayList<>();
        String line;
        int lineNumber = 1;
        int nextSortOrder = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty()) {
                continue;
            }

            try {
                String[] values = parseCsvLine(line);
                Test test = processCsvLine(values, testNameIndex, testSectionIndex, sampleTypeIndex, loincIndex,
                        isActiveIndex, isOrderableIndex, sortOrderIndex, unitOfMeasureIndex, englishNameIndex,
                        frenchNameIndex, lineNumber, fileName, nextSortOrder);
                if (test != null) {
                    processedTests.add(test);
                    nextSortOrder++;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }

        // Refresh caches
        testService.refreshTestNames();
        DisplayListService.getInstance().refreshLists();

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Successfully loaded " + processedTests.size() + " tests from " + fileName);
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
        boolean hasTestSectionColumn = false;

        for (String header : headers) {
            if ("testName".equalsIgnoreCase(header)) {
                hasTestNameColumn = true;
            }
            if ("testSection".equalsIgnoreCase(header)) {
                hasTestSectionColumn = true;
            }
        }

        if (!hasTestNameColumn) {
            throw new IllegalArgumentException(
                    "Test configuration file " + fileName + " must have a 'testName' column");
        }
        if (!hasTestSectionColumn) {
            throw new IllegalArgumentException(
                    "Test configuration file " + fileName + " must have a 'testSection' column");
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

    private Test processCsvLine(String[] values, int testNameIndex, int testSectionIndex, int sampleTypeIndex,
            int loincIndex, int isActiveIndex, int isOrderableIndex, int sortOrderIndex, int unitOfMeasureIndex,
            int englishNameIndex, int frenchNameIndex, int lineNumber, String fileName, int defaultSortOrder) {

        String testName = getValueOrEmpty(values, testNameIndex);
        String testSectionName = getValueOrEmpty(values, testSectionIndex);

        if (testName.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Skipping row " + lineNumber + " in " + fileName + " with missing testName");
            return null;
        }

        if (testSectionName.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Skipping row " + lineNumber + " in " + fileName + " with missing testSection");
            return null;
        }

        // Find or validate test section
        TestSection testSection = testSectionService.getTestSectionByName(testSectionName);
        if (testSection == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine", "Test section '" + testSectionName
                    + "' not found in line " + lineNumber + " of " + fileName + ". Skipping.");
            return null;
        }

        // Check if test already exists
        Test existingTest = testService.getTestByDescription(testName);

        Test test;
        if (existingTest != null) {
            test = updateTest(existingTest, values, testName, testSection, loincIndex, isActiveIndex, isOrderableIndex,
                    sortOrderIndex, unitOfMeasureIndex, englishNameIndex, frenchNameIndex, defaultSortOrder);
            LogEvent.logInfo(this.getClass().getSimpleName(), "processCsvLine", "Updated existing test: " + testName);
        } else {
            test = createTest(values, testName, testSection, loincIndex, isActiveIndex, isOrderableIndex,
                    sortOrderIndex, unitOfMeasureIndex, englishNameIndex, frenchNameIndex, defaultSortOrder);
            LogEvent.logInfo(this.getClass().getSimpleName(), "processCsvLine", "Created new test: " + testName);
        }

        // Handle sample type mappings
        String sampleTypes = getValueOrEmpty(values, sampleTypeIndex);
        if (!sampleTypes.isEmpty() && test != null) {
            createSampleTypeMappings(test, sampleTypes, lineNumber, fileName);
        }

        return test;
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value : "";
        }
        return "";
    }

    private Test updateTest(Test test, String[] values, String testName, TestSection testSection, int loincIndex,
            int isActiveIndex, int isOrderableIndex, int sortOrderIndex, int unitOfMeasureIndex, int englishNameIndex,
            int frenchNameIndex, int defaultSortOrder) {

        test.setDescription(testName);
        test.setTestSection(testSection);

        // Update LOINC
        String loinc = getValueOrEmpty(values, loincIndex);
        if (!loinc.isEmpty()) {
            test.setLoinc(loinc);
        }

        // Update active status
        String isActive = getValueOrEmpty(values, isActiveIndex);
        if (!isActive.isEmpty()) {
            test.setIsActive("Y".equalsIgnoreCase(isActive) || "true".equalsIgnoreCase(isActive) ? "Y" : "N");
        }

        // Update orderable status
        String isOrderable = getValueOrEmpty(values, isOrderableIndex);
        if (!isOrderable.isEmpty()) {
            test.setOrderable("Y".equalsIgnoreCase(isOrderable) || "true".equalsIgnoreCase(isOrderable));
        }

        // Update sort order
        String sortOrderStr = getValueOrEmpty(values, sortOrderIndex);
        if (!sortOrderStr.isEmpty()) {
            test.setSortOrder(sortOrderStr);
        }

        // Update unit of measure
        String uomName = getValueOrEmpty(values, unitOfMeasureIndex);
        if (!uomName.isEmpty()) {
            UnitOfMeasure uom = findUnitOfMeasure(uomName);
            if (uom != null) {
                test.setUnitOfMeasure(uom);
            }
        }

        // Update localization
        String englishName = getValueOrEmpty(values, englishNameIndex);
        String frenchName = getValueOrEmpty(values, frenchNameIndex);
        updateTestLocalization(test, englishName, frenchName, testName);

        test.setSysUserId("1");
        testService.update(test);
        return test;
    }

    private Test createTest(String[] values, String testName, TestSection testSection, int loincIndex,
            int isActiveIndex, int isOrderableIndex, int sortOrderIndex, int unitOfMeasureIndex, int englishNameIndex,
            int frenchNameIndex, int defaultSortOrder) {

        // Create localization first
        String englishName = getValueOrEmpty(values, englishNameIndex);
        String frenchName = getValueOrEmpty(values, frenchNameIndex);

        if (englishName.isEmpty()) {
            englishName = testName;
        }
        if (frenchName.isEmpty()) {
            frenchName = englishName;
        }

        Localization localization = new Localization();
        localization.setDescription("test name");
        localization.setEnglish(englishName);
        localization.setFrench(frenchName);
        localization.setSysUserId("1");
        localizationService.insert(localization);

        // Create reporting name localization (same as test name by default)
        Localization reportingLocalization = new Localization();
        reportingLocalization.setDescription("test reporting name");
        reportingLocalization.setEnglish(englishName);
        reportingLocalization.setFrench(frenchName);
        reportingLocalization.setSysUserId("1");
        localizationService.insert(reportingLocalization);

        // Create test
        Test test = new Test();
        test.setDescription(testName);
        test.setTestSection(testSection);
        test.setLocalizedTestName(localization);
        test.setLocalizedReportingName(reportingLocalization);
        test.setGuid(UUID.randomUUID().toString());

        // Set LOINC
        String loinc = getValueOrEmpty(values, loincIndex);
        if (!loinc.isEmpty()) {
            test.setLoinc(loinc);
        }

        // Set active status
        String isActive = getValueOrEmpty(values, isActiveIndex);
        if (!isActive.isEmpty()) {
            test.setIsActive("Y".equalsIgnoreCase(isActive) || "true".equalsIgnoreCase(isActive) ? "Y" : "N");
        } else {
            test.setIsActive("Y");
        }

        // Set orderable status
        String isOrderable = getValueOrEmpty(values, isOrderableIndex);
        if (!isOrderable.isEmpty()) {
            test.setOrderable("Y".equalsIgnoreCase(isOrderable) || "true".equalsIgnoreCase(isOrderable));
        } else {
            test.setOrderable(true);
        }

        // Set sort order
        String sortOrderStr = getValueOrEmpty(values, sortOrderIndex);
        if (!sortOrderStr.isEmpty()) {
            test.setSortOrder(sortOrderStr);
        } else {
            test.setSortOrder(String.valueOf(defaultSortOrder));
        }

        // Set unit of measure
        String uomName = getValueOrEmpty(values, unitOfMeasureIndex);
        if (!uomName.isEmpty()) {
            UnitOfMeasure uom = findUnitOfMeasure(uomName);
            if (uom != null) {
                test.setUnitOfMeasure(uom);
            }
        }

        // Set other defaults
        test.setIsReportable("Y");
        test.setSysUserId("1");

        String testId = testService.insert(test);
        test.setId(testId);
        return test;
    }

    private void updateTestLocalization(Test test, String englishName, String frenchName, String defaultName) {
        if (englishName.isEmpty() && frenchName.isEmpty()) {
            return;
        }

        Localization localization = test.getLocalizedTestName();
        if (localization != null) {
            if (!englishName.isEmpty()) {
                localization.setEnglish(englishName);
            }
            if (!frenchName.isEmpty()) {
                localization.setFrench(frenchName);
            }
            localization.setSysUserId("1");
            localizationService.update(localization);
        }

        Localization reportingLocalization = test.getLocalizedReportingName();
        if (reportingLocalization != null) {
            if (!englishName.isEmpty()) {
                reportingLocalization.setEnglish(englishName);
            }
            if (!frenchName.isEmpty()) {
                reportingLocalization.setFrench(frenchName);
            }
            reportingLocalization.setSysUserId("1");
            localizationService.update(reportingLocalization);
        }
    }

    private UnitOfMeasure findUnitOfMeasure(String uomName) {
        List<UnitOfMeasure> allUom = unitOfMeasureService.getAll();
        for (UnitOfMeasure uom : allUom) {
            if (uom.getUnitOfMeasureName() != null && uom.getUnitOfMeasureName().equalsIgnoreCase(uomName)) {
                return uom;
            }
            if (uom.getDescription() != null && uom.getDescription().equalsIgnoreCase(uomName)) {
                return uom;
            }
        }
        return null;
    }

    private void createSampleTypeMappings(Test test, String sampleTypes, int lineNumber, String fileName) {
        // Sample types can be separated by |
        String[] sampleTypeNames = sampleTypes.split("\\|");

        for (String sampleTypeName : sampleTypeNames) {
            sampleTypeName = sampleTypeName.trim();
            if (sampleTypeName.isEmpty()) {
                continue;
            }

            TypeOfSample sampleType = findSampleType(sampleTypeName);
            if (sampleType == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "createSampleTypeMappings", "Sample type '"
                        + sampleTypeName + "' not found for test in line " + lineNumber + " of " + fileName);
                continue;
            }

            // Check if mapping already exists
            if (!mappingExists(test.getId(), sampleType.getId())) {
                TypeOfSampleTest mapping = new TypeOfSampleTest();
                mapping.setTestId(test.getId());
                mapping.setTypeOfSampleId(sampleType.getId());
                mapping.setSysUserId("1");
                typeOfSampleTestService.insert(mapping);
                LogEvent.logDebug(this.getClass().getSimpleName(), "createSampleTypeMappings", "Created mapping: test '"
                        + test.getDescription() + "' -> sample type '" + sampleTypeName + "'");
            }
        }
    }

    private TypeOfSample findSampleType(String sampleTypeName) {
        List<TypeOfSample> allSampleTypes = typeOfSampleService.getAllTypeOfSamples();

        for (TypeOfSample sampleType : allSampleTypes) {
            if (sampleType.getLocalizedName() != null
                    && sampleType.getLocalizedName().equalsIgnoreCase(sampleTypeName)) {
                return sampleType;
            }
            if (sampleType.getDescription() != null && sampleType.getDescription().equalsIgnoreCase(sampleTypeName)) {
                return sampleType;
            }
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
