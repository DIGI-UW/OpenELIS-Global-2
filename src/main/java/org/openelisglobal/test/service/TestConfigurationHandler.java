package org.openelisglobal.test.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationValueService;
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
 * <p>
 * Expected CSV format:
 * testName,testSection,sampleType,loinc,isActive,isOrderable,sortOrder,unitOfMeasure,localization:en,localization:fr
 * Glucose,Biochemistry,Serum,2345-7,Y,Y,1,mg/dL,Glucose,Glucose
 * Hemoglobin,Hematology,Whole Blood,718-7,Y,Y,2,g/dL,Hemoglobin,Hémoglobine HIV
 * Rapid Test,Serology,Plasma|Serum|Whole Blood,68961-2,Y,Y,3,,HIV Rapid
 * Test,Test Rapide VIH
 * <p>
 * Sample Type Handling: - When multiple sample types are specified (separated
 * by |), separate tests are created for each sample type - Test names are
 * suffixed with the sample type: "TestName(SampleType)" (no space) - Example:
 * "Rapid Test,Serology,Plasma|Serum|Whole Blood" creates: * "Rapid
 * Test(Plasma)" with sample type Plasma * "Rapid Test(Serum)" with sample type
 * Serum * "Rapid Test(Whole Blood)" with sample type Whole Blood - Each test
 * has only one sample type mapping
 * <p>
 * Notes: - First line is the header (required) - testName and testSection are
 * required fields - sampleType is optional but recommended (can specify
 * multiple separated by |) - loinc is optional but recommended for
 * interoperability - isActive defaults to "Y" if not specified - isOrderable
 * defaults to "Y" if not specified - sortOrder is optional (auto-assigned if
 * not provided) - unitOfMeasure is optional - localization:xx columns (where xx
 * is a locale code like en, fr, es) provide translations - If no localization
 * columns are provided, testName is used as the default value for the fallback
 * locale (en) - Existing tests with matching description will be updated
 */
@Component
public class TestConfigurationHandler implements DomainConfigurationHandler {

    private static final String LOCALIZATION_COLUMN_PREFIX = "localization:";

    @Autowired
    private TestService testService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private LocalizationValueService localizationValueService;

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

        // Detect localization columns (localization:en, localization:fr, etc.)
        Map<String, Integer> localizationColumns = detectLocalizationColumns(headers);

        List<Test> processedTests = new ArrayList<>();
        String line;
        int lineNumber = 1;
        int nextSortOrder = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            // Skip empty lines and comments (lines starting with #)
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            try {
                String[] values = parseCsvLine(line);
                Test test = processCsvLine(values, testNameIndex, testSectionIndex, sampleTypeIndex, loincIndex,
                        isActiveIndex, isOrderableIndex, sortOrderIndex, unitOfMeasureIndex, localizationColumns,
                        lineNumber, fileName, nextSortOrder);
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

    /**
     * Detects localization columns from headers. Columns must be in the format
     * "localization:xx" where xx is a locale code (e.g., en, fr, es).
     *
     * @param headers the CSV header row
     * @return map of locale code to column index
     */
    private Map<String, Integer> detectLocalizationColumns(String[] headers) {
        Map<String, Integer> localizationColumns = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim().toLowerCase();
            if (header.startsWith(LOCALIZATION_COLUMN_PREFIX)) {
                String locale = header.substring(LOCALIZATION_COLUMN_PREFIX.length());
                if (!locale.isEmpty()) {
                    localizationColumns.put(locale, i);
                }
            }
        }
        return localizationColumns;
    }

    private Test processCsvLine(String[] values, int testNameIndex, int testSectionIndex, int sampleTypeIndex,
            int loincIndex, int isActiveIndex, int isOrderableIndex, int sortOrderIndex, int unitOfMeasureIndex,
            Map<String, Integer> localizationColumns, int lineNumber, String fileName, int defaultSortOrder) {

        String baseTestName = getValueOrEmpty(values, testNameIndex);
        String testSectionName = getValueOrEmpty(values, testSectionIndex);

        if (baseTestName.isEmpty()) {
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

        String sampleTypes = getValueOrEmpty(values, sampleTypeIndex);
        if (!sampleTypes.isEmpty()) {
            String[] sampleTypeNames = sampleTypes.split("\\|");
            Test lastCreatedTest = null;

            for (String sampleTypeName : sampleTypeNames) {
                sampleTypeName = sampleTypeName.trim();
                if (sampleTypeName.isEmpty()) {
                    continue;
                }

                TypeOfSample sampleType = findSampleType(sampleTypeName);
                if (sampleType == null) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                            "Sample type '" + sampleTypeName + "' not found for test in line " + lineNumber + " of "
                                    + fileName + ". Skipping this sample type.");
                    continue;
                }

                String testNameWithSampleType = baseTestName + "(" + sampleTypeName + ")"; // to match the
                                                                                           // buildAugmentedTestName(Test
                                                                                           // test){} method in
                                                                                           // TestServiceImpl
                // Use normalized matching to find existing tests that should be overridden
                // (e.g., "Stat-Pak(Plasma)" matches "Stat PaK(Plasma)")
                Test existingTest = testService.getTestByNormalizedDescription(testNameWithSampleType);

                Test test;
                if (existingTest != null) {
                    test = updateTest(existingTest, values, testNameWithSampleType, testSection, loincIndex,
                            isActiveIndex, isOrderableIndex, sortOrderIndex, unitOfMeasureIndex, localizationColumns,
                            defaultSortOrder);
                    LogEvent.logInfo(this.getClass().getSimpleName(), "processCsvLine",
                            "Updated existing test: " + testNameWithSampleType);
                } else {
                    test = createTest(values, testNameWithSampleType, testSection, loincIndex, isActiveIndex,
                            isOrderableIndex, sortOrderIndex, unitOfMeasureIndex, localizationColumns,
                            defaultSortOrder);
                    LogEvent.logInfo(this.getClass().getSimpleName(), "processCsvLine",
                            "Created new test: " + testNameWithSampleType);
                }

                if (test != null) {
                    createSingleSampleTypeMapping(test, sampleType);
                    lastCreatedTest = test;
                }
            }

            return lastCreatedTest;
        } else {
            Test existingTest = testService.getTestByNormalizedDescription(baseTestName);

            Test test;
            if (existingTest != null) {
                test = updateTest(existingTest, values, baseTestName, testSection, loincIndex, isActiveIndex,
                        isOrderableIndex, sortOrderIndex, unitOfMeasureIndex, localizationColumns,
                        defaultSortOrder);
                LogEvent.logInfo(this.getClass().getSimpleName(), "processCsvLine",
                        "Updated existing test: " + baseTestName);
            } else {
                test = createTest(values, baseTestName, testSection, loincIndex, isActiveIndex, isOrderableIndex,
                        sortOrderIndex, unitOfMeasureIndex, localizationColumns, defaultSortOrder);
                LogEvent.logInfo(this.getClass().getSimpleName(), "processCsvLine",
                        "Created new test: " + baseTestName);
            }

            return test;
        }
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value : "";
        }
        return "";
    }

    private Test updateTest(Test test, String[] values, String testName, TestSection testSection, int loincIndex,
            int isActiveIndex, int isOrderableIndex, int sortOrderIndex, int unitOfMeasureIndex,
            Map<String, Integer> localizationColumns, int defaultSortOrder) {

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

        test.setSysUserId("1");

        // Handle localization
        processTestLocalization(test, values, testName, localizationColumns);

        testService.update(test);
        return test;
    }

    private Test createTest(String[] values, String testName, TestSection testSection, int loincIndex,
            int isActiveIndex, int isOrderableIndex, int sortOrderIndex, int unitOfMeasureIndex,
            Map<String, Integer> localizationColumns, int defaultSortOrder) {

        // Build translations map
        Map<String, String> translations = buildTranslationsMap(values, testName, localizationColumns);

        // Create localization first
        Localization localization = new Localization();
        localization.setDescription("test name");
        localization.setEnglish(translations.getOrDefault("en", testName));
        localization.setFrench(translations.getOrDefault("fr", translations.getOrDefault("en", testName)));
        localization.setSysUserId("1");
        String localizationId = localizationService.insert(localization);
        localization.setId(localizationId);

        // Set all translations using the service (including any beyond en/fr)
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            localizationValueService.setTranslation(localizationId, entry.getKey(), entry.getValue());
        }

        // Create reporting name localization (same as test name by default)
        Localization reportingLocalization = new Localization();
        reportingLocalization.setDescription("test reporting name");
        reportingLocalization.setEnglish(translations.getOrDefault("en", testName));
        reportingLocalization.setFrench(translations.getOrDefault("fr", translations.getOrDefault("en", testName)));
        reportingLocalization.setSysUserId("1");
        String reportingLocalizationId = localizationService.insert(reportingLocalization);
        reportingLocalization.setId(reportingLocalizationId);

        // Set all translations for reporting name
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            localizationValueService.setTranslation(reportingLocalizationId, entry.getKey(), entry.getValue());
        }

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

    /**
     * Builds a map of translations from localization columns. If no translations
     * are provided, uses the default name as the fallback (en) value.
     */
    private Map<String, String> buildTranslationsMap(String[] values, String defaultName,
            Map<String, Integer> localizationColumns) {
        Map<String, String> translations = new HashMap<>();

        if (localizationColumns.isEmpty()) {
            // No localization columns provided - use defaultName as the fallback (en) value
            translations.put("en", defaultName);
        } else {
            // Process each localization column
            for (Map.Entry<String, Integer> entry : localizationColumns.entrySet()) {
                String locale = entry.getKey();
                String translationValue = getValueOrEmpty(values, entry.getValue());
                if (!translationValue.isEmpty()) {
                    translations.put(locale, translationValue);
                }
            }

            // If no valid translations found, use defaultName as fallback
            if (translations.isEmpty()) {
                translations.put("en", defaultName);
            }
        }

        return translations;
    }

    /**
     * Processes localization columns and sets up translations for the test.
     */
    private void processTestLocalization(Test test, String[] values, String testName,
            Map<String, Integer> localizationColumns) {

        Map<String, String> translations = buildTranslationsMap(values, testName, localizationColumns);

        // Update test name localization
        Localization localization = test.getLocalizedTestName();
        if (localization != null) {
            String localizationId = localization.getId();
            for (Map.Entry<String, String> entry : translations.entrySet()) {
                localizationValueService.setTranslation(localizationId, entry.getKey(), entry.getValue());
            }
        }

        // Update reporting name localization
        Localization reportingLocalization = test.getLocalizedReportingName();
        if (reportingLocalization != null) {
            String reportingLocalizationId = reportingLocalization.getId();
            for (Map.Entry<String, String> entry : translations.entrySet()) {
                localizationValueService.setTranslation(reportingLocalizationId, entry.getKey(), entry.getValue());
            }
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

    private void createSingleSampleTypeMapping(Test test, TypeOfSample sampleType) {
        if (!mappingExists(test.getId(), sampleType.getId())) {
            TypeOfSampleTest mapping = new TypeOfSampleTest();
            mapping.setTestId(test.getId());
            mapping.setTypeOfSampleId(sampleType.getId());
            mapping.setSysUserId("1");
            typeOfSampleTestService.insert(mapping);
            LogEvent.logDebug(this.getClass().getSimpleName(), "createSingleSampleTypeMapping",
                    "Created mapping: test '" + test.getDescription() + "' -> sample type '"
                            + sampleType.getLocalizedName() + "'");
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
