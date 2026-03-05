package org.openelisglobal.test.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationValueService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handler for loading test section configuration files. Supports CSV format for
 * defining laboratory test sections/departments.
 *
 * Expected CSV format:
 * testSectionName,description,isActive,sortOrder,isExternal,localization:en,localization:fr
 * Hematology,Hematology Department,Y,1,N,Hematology,Hématologie
 * Biochemistry,Biochemistry Department,Y,2,N,Biochemistry,Biochimie
 * Serology,Serology Department,Y,3,N,Serology,Sérologie
 *
 * Notes: - First line is the header (required) - testSectionName is required -
 * description defaults to testSectionName if not provided - isActive defaults
 * to "Y" if not specified - sortOrder is optional (auto-assigned if not
 * provided) - isExternal defaults to "N" if not specified - localization:xx
 * columns (where xx is a locale code like en, fr, es) provide translations - If
 * no localization columns are provided, testSectionName is used as the default
 * value for the fallback locale (en) - Existing test sections with matching
 * name will be updated
 */
@Component
public class TestSectionConfigurationHandler implements DomainConfigurationHandler {

    private static final String LOCALIZATION_COLUMN_PREFIX = "localization:";

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private LocalizationValueService localizationValueService;

    @Override
    public String getDomainName() {
        return "test-sections";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 100; // Base entity - load early, before tests
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Read and validate header
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Test section configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        validateHeaders(headers, fileName);

        // Get column indices
        int testSectionNameIndex = findColumnIndex(headers, "testSectionName");
        int descriptionIndex = findColumnIndex(headers, "description");
        int isActiveIndex = findColumnIndex(headers, "isActive");
        int sortOrderIndex = findColumnIndex(headers, "sortOrder");
        int isExternalIndex = findColumnIndex(headers, "isExternal");

        // Detect localization columns (localization:en, localization:fr, etc.)
        Map<String, Integer> localizationColumns = detectLocalizationColumns(headers);

        List<TestSection> processedSections = new ArrayList<>();
        String line;
        int lineNumber = 1;
        int nextSortOrder = getNextAvailableSortOrder();

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            // Skip empty lines and comments
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            try {
                String[] values = parseCsvLine(line);
                TestSection section = processCsvLine(values, testSectionNameIndex, descriptionIndex, isActiveIndex,
                        sortOrderIndex, isExternalIndex, localizationColumns, lineNumber, fileName, nextSortOrder);
                if (section != null) {
                    processedSections.add(section);
                    nextSortOrder++;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }

        // Refresh caches
        testSectionService.refreshNames();
        DisplayListService.getInstance().refreshLists();

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Successfully loaded " + processedSections.size() + " test sections from " + fileName);
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
        boolean hasTestSectionNameColumn = false;

        for (String header : headers) {
            if ("testSectionName".equalsIgnoreCase(header)) {
                hasTestSectionNameColumn = true;
                break;
            }
        }

        if (!hasTestSectionNameColumn) {
            throw new IllegalArgumentException(
                    "Test section configuration file " + fileName + " must have a 'testSectionName' column");
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

    private int getNextAvailableSortOrder() {
        List<TestSection> allSections = testSectionService.getAllTestSections();
        int maxSortOrder = 0;
        for (TestSection section : allSections) {
            if (section.getSortOrderInt() > maxSortOrder) {
                maxSortOrder = section.getSortOrderInt();
            }
        }
        return maxSortOrder + 1;
    }

    private TestSection processCsvLine(String[] values, int testSectionNameIndex, int descriptionIndex,
            int isActiveIndex, int sortOrderIndex, int isExternalIndex, Map<String, Integer> localizationColumns,
            int lineNumber, String fileName, int defaultSortOrder) {

        String testSectionName = getValueOrEmpty(values, testSectionNameIndex);

        if (testSectionName.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Skipping row " + lineNumber + " in " + fileName + " with missing testSectionName");
            return null;
        }

        // Check if test section already exists
        TestSection existingSection = testSectionService.getTestSectionByName(testSectionName);

        if (existingSection != null) {
            updateTestSection(existingSection, values, testSectionName, descriptionIndex, isActiveIndex, sortOrderIndex,
                    isExternalIndex, localizationColumns, defaultSortOrder);
            testSectionService.update(existingSection);
            LogEvent.logInfo(this.getClass().getSimpleName(), "processCsvLine",
                    "Updated existing test section: " + testSectionName);
            return existingSection;
        } else {
            return createTestSection(values, testSectionName, descriptionIndex, isActiveIndex, sortOrderIndex,
                    isExternalIndex, localizationColumns, defaultSortOrder);
        }
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value : "";
        }
        return "";
    }

    private void updateTestSection(TestSection section, String[] values, String testSectionName, int descriptionIndex,
            int isActiveIndex, int sortOrderIndex, int isExternalIndex, Map<String, Integer> localizationColumns,
            int defaultSortOrder) {

        section.setTestSectionName(testSectionName);

        // Update description
        String description = getValueOrEmpty(values, descriptionIndex);
        if (!description.isEmpty()) {
            section.setDescription(description);
        }

        // Update active status
        String isActive = getValueOrEmpty(values, isActiveIndex);
        if (!isActive.isEmpty()) {
            section.setIsActive("Y".equalsIgnoreCase(isActive) || "true".equalsIgnoreCase(isActive) ? "Y" : "N");
        }

        // Update sort order
        String sortOrderStr = getValueOrEmpty(values, sortOrderIndex);
        if (!sortOrderStr.isEmpty()) {
            try {
                section.setSortOrderInt(Integer.parseInt(sortOrderStr));
            } catch (NumberFormatException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "updateTestSection",
                        "Invalid sortOrder value: " + sortOrderStr);
            }
        }

        // Update external flag
        String isExternal = getValueOrEmpty(values, isExternalIndex);
        if (!isExternal.isEmpty()) {
            section.setIsExternal("Y".equalsIgnoreCase(isExternal) || "true".equalsIgnoreCase(isExternal) ? "Y" : "N");
        }

        section.setSysUserId("1");

        // Handle localization
        processLocalization(section, values, testSectionName, localizationColumns);
    }

    private TestSection createTestSection(String[] values, String testSectionName, int descriptionIndex,
            int isActiveIndex, int sortOrderIndex, int isExternalIndex, Map<String, Integer> localizationColumns,
            int defaultSortOrder) {

        // Build translations map
        Map<String, String> translations = buildTranslationsMap(values, testSectionName, localizationColumns);

        // Create localization first
        Localization localization = new Localization();
        localization.setDescription("test section name");
        localization.setEnglish(translations.getOrDefault("en", testSectionName));
        localization.setFrench(translations.getOrDefault("fr", translations.getOrDefault("en", testSectionName)));
        localization.setSysUserId("1");
        String localizationId = localizationService.insert(localization);
        localization.setId(localizationId);

        // Set all translations using the service (including any beyond en/fr)
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            localizationValueService.setTranslation(localizationId, entry.getKey(), entry.getValue());
        }

        // Create test section
        TestSection section = new TestSection();
        section.setTestSectionName(testSectionName);
        section.setLocalization(localization);

        // Set description
        String description = getValueOrEmpty(values, descriptionIndex);
        if (!description.isEmpty()) {
            section.setDescription(description);
        } else {
            section.setDescription(testSectionName);
        }

        // Set active status
        String isActive = getValueOrEmpty(values, isActiveIndex);
        if (!isActive.isEmpty()) {
            section.setIsActive("Y".equalsIgnoreCase(isActive) || "true".equalsIgnoreCase(isActive) ? "Y" : "N");
        } else {
            section.setIsActive("Y");
        }

        // Set sort order
        String sortOrderStr = getValueOrEmpty(values, sortOrderIndex);
        if (!sortOrderStr.isEmpty()) {
            try {
                section.setSortOrderInt(Integer.parseInt(sortOrderStr));
            } catch (NumberFormatException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "createTestSection",
                        "Invalid sortOrder value: " + sortOrderStr + ", using default: " + defaultSortOrder);
                section.setSortOrderInt(defaultSortOrder);
            }
        } else {
            section.setSortOrderInt(defaultSortOrder);
        }

        // Set external flag
        String isExternal = getValueOrEmpty(values, isExternalIndex);
        if (!isExternal.isEmpty()) {
            section.setIsExternal("Y".equalsIgnoreCase(isExternal) || "true".equalsIgnoreCase(isExternal) ? "Y" : "N");
        } else {
            section.setIsExternal("N");
        }

        section.setSysUserId("1");

        String sectionId = testSectionService.insert(section);
        section.setId(sectionId);

        LogEvent.logInfo(this.getClass().getSimpleName(), "createTestSection",
                "Created new test section: " + testSectionName);

        return section;
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
     * Processes localization columns and sets up translations for the test section.
     */
    private void processLocalization(TestSection section, String[] values, String testSectionName,
            Map<String, Integer> localizationColumns) {

        Map<String, String> translations = buildTranslationsMap(values, testSectionName, localizationColumns);

        Localization localization = section.getLocalization();
        if (localization == null) {
            // Create new localization
            localization = new Localization();
            localization.setDescription("test section name");
            localization.setEnglish(translations.getOrDefault("en", testSectionName));
            localization.setFrench(translations.getOrDefault("fr", translations.getOrDefault("en", testSectionName)));
            localization.setSysUserId("1");
            String localizationId = localizationService.insert(localization);
            localization.setId(localizationId);
            section.setLocalization(localization);

            // Set all translations
            for (Map.Entry<String, String> entry : translations.entrySet()) {
                localizationValueService.setTranslation(localizationId, entry.getKey(), entry.getValue());
            }
        } else {
            // Update existing localization translations
            String localizationId = localization.getId();
            for (Map.Entry<String, String> entry : translations.entrySet()) {
                localizationValueService.setTranslation(localizationId, entry.getKey(), entry.getValue());
            }
        }
    }
}
