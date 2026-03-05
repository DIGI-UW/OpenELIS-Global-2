package org.openelisglobal.typeofsample.service;

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
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handler for loading sample type (TypeOfSample) configuration files. Supports
 * CSV format for defining sample types.
 *
 * Expected CSV format:
 * description,localAbbreviation,domain,isActive,sortOrder,localization:en,localization:fr
 * Whole Blood,WB,H,Y,1,Whole Blood,Sang Total Serum,SER,H,Y,2,Serum,Sérum
 * Plasma,PLS,H,Y,3,Plasma,Plasma Urine,UR,H,Y,4,Urine,Urine
 *
 * Notes: - First line is the header (required) - description and
 * localAbbreviation are required fields - domain defaults to "H" (Human) if not
 * specified - isActive defaults to "Y" if not specified - sortOrder is optional
 * (auto-assigned if not provided) - localization:xx columns (where xx is a
 * locale code like en, fr, es) provide translations - If no localization
 * columns are provided, description is used as the default value for the
 * fallback locale (en) - Existing sample types with matching localAbbreviation
 * and domain will be updated
 */
@Component
public class TypeOfSampleConfigurationHandler implements DomainConfigurationHandler {

    private static final String DEFAULT_DOMAIN = "H"; // Human
    private static final String LOCALIZATION_COLUMN_PREFIX = "localization:";

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private LocalizationValueService localizationValueService;

    @Override
    public String getDomainName() {
        return "sample-types";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 100; // Base entity - load early
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Read and validate header
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Sample type configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        validateHeaders(headers, fileName);

        // Get column indices
        int descriptionIndex = findColumnIndex(headers, "description");
        int localAbbreviationIndex = findColumnIndex(headers, "localAbbreviation");
        int domainIndex = findColumnIndex(headers, "domain");
        int isActiveIndex = findColumnIndex(headers, "isActive");
        int sortOrderIndex = findColumnIndex(headers, "sortOrder");

        // Detect localization columns (localization:en, localization:fr, etc.)
        Map<String, Integer> localizationColumns = detectLocalizationColumns(headers);

        List<TypeOfSample> processedSampleTypes = new ArrayList<>();
        String line;
        int lineNumber = 1; // Start at 1 since we already read the header
        int nextSortOrder = getNextAvailableSortOrder();

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            // Skip empty lines and comments
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            try {
                String[] values = parseCsvLine(line);
                TypeOfSample sampleType = processCsvLine(values, descriptionIndex, localAbbreviationIndex, domainIndex,
                        isActiveIndex, sortOrderIndex, localizationColumns, lineNumber, fileName, nextSortOrder);
                if (sampleType != null) {
                    processedSampleTypes.add(sampleType);
                    nextSortOrder++;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }

        // Clear caches and refresh display lists
        typeOfSampleService.clearCache();
        DisplayListService.getInstance().refreshLists();

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Successfully loaded " + processedSampleTypes.size() + " sample types from " + fileName);
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
        boolean hasDescriptionColumn = false;
        boolean hasLocalAbbreviationColumn = false;

        for (String header : headers) {
            if ("description".equalsIgnoreCase(header)) {
                hasDescriptionColumn = true;
            }
            if ("localAbbreviation".equalsIgnoreCase(header)) {
                hasLocalAbbreviationColumn = true;
            }
        }

        if (!hasDescriptionColumn) {
            throw new IllegalArgumentException(
                    "Sample type configuration file " + fileName + " must have a 'description' column");
        }
        if (!hasLocalAbbreviationColumn) {
            throw new IllegalArgumentException(
                    "Sample type configuration file " + fileName + " must have a 'localAbbreviation' column");
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
        List<TypeOfSample> allSampleTypes = typeOfSampleService.getAllTypeOfSamples();
        int maxSortOrder = 0;
        for (TypeOfSample sampleType : allSampleTypes) {
            if (sampleType.getSortOrder() > maxSortOrder) {
                maxSortOrder = sampleType.getSortOrder();
            }
        }
        return maxSortOrder + 1;
    }

    private TypeOfSample processCsvLine(String[] values, int descriptionIndex, int localAbbreviationIndex,
            int domainIndex, int isActiveIndex, int sortOrderIndex, Map<String, Integer> localizationColumns,
            int lineNumber, String fileName, int defaultSortOrder) {

        // Get required fields
        String description = getValueOrEmpty(values, descriptionIndex);
        String localAbbreviation = getValueOrEmpty(values, localAbbreviationIndex);

        if (description.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Skipping row " + lineNumber + " in " + fileName + " with missing description");
            return null;
        }

        if (localAbbreviation.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Skipping row " + lineNumber + " in " + fileName + " with missing localAbbreviation");
            return null;
        }

        // Get optional fields
        String domain = getValueOrEmpty(values, domainIndex);
        if (domain.isEmpty()) {
            domain = DEFAULT_DOMAIN;
        }

        // Check if sample type already exists by localAbbreviation and domain
        TypeOfSample existingSampleType = typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain(localAbbreviation,
                domain);

        // If not found by abbreviation, also check by description and domain
        if (existingSampleType == null) {
            existingSampleType = findSampleTypeByDescriptionAndDomain(description, domain);
        }

        if (existingSampleType != null) {
            // Update existing sample type
            updateSampleTypeFromCsv(existingSampleType, values, description, localAbbreviation, isActiveIndex,
                    sortOrderIndex, localizationColumns, defaultSortOrder);
            typeOfSampleService.update(existingSampleType);
            LogEvent.logInfo(this.getClass().getSimpleName(), "processCsvLine",
                    "Updated existing sample type: " + description + " (" + localAbbreviation + ")");
            return existingSampleType;
        } else {
            // Create new sample type
            return createSampleType(values, description, localAbbreviation, domain, isActiveIndex, sortOrderIndex,
                    localizationColumns, defaultSortOrder);
        }
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value : "";
        }
        return "";
    }

    private TypeOfSample findSampleTypeByDescriptionAndDomain(String description, String domain) {
        TypeOfSample searchType = new TypeOfSample();
        searchType.setDescription(description);
        searchType.setDomain(domain);
        return typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);
    }

    private void updateSampleTypeFromCsv(TypeOfSample sampleType, String[] values, String description,
            String localAbbreviation, int isActiveIndex, int sortOrderIndex, Map<String, Integer> localizationColumns,
            int defaultSortOrder) {

        sampleType.setDescription(description);
        sampleType.setLocalAbbreviation(localAbbreviation);

        // Update active status
        String isActive = getValueOrEmpty(values, isActiveIndex);
        if (!isActive.isEmpty()) {
            sampleType.setActive("Y".equalsIgnoreCase(isActive) || "true".equalsIgnoreCase(isActive));
        }

        // Update sort order
        String sortOrderStr = getValueOrEmpty(values, sortOrderIndex);
        if (!sortOrderStr.isEmpty()) {
            try {
                sampleType.setSortOrder(Integer.parseInt(sortOrderStr));
            } catch (NumberFormatException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "updateSampleTypeFromCsv",
                        "Invalid sortOrder value: " + sortOrderStr + ", using default");
            }
        }

        sampleType.setSysUserId("1"); // System user for configuration loading

        // Handle localization
        processLocalization(sampleType, values, description, localizationColumns);
    }

    private TypeOfSample createSampleType(String[] values, String description, String localAbbreviation, String domain,
            int isActiveIndex, int sortOrderIndex, Map<String, Integer> localizationColumns, int defaultSortOrder) {

        // Build translations map
        Map<String, String> translations = buildTranslationsMap(values, description, localizationColumns);

        // Create localization first
        Localization localization = new Localization();
        localization.setDescription("sampleType name");
        // Set legacy en/fr fields for compatibility
        localization.setEnglish(translations.getOrDefault("en", description));
        localization.setFrench(translations.getOrDefault("fr", translations.getOrDefault("en", description)));
        localization.setSysUserId("1");
        String localizationId = localizationService.insert(localization);
        localization.setId(localizationId);

        // Set all translations using the service (including any beyond en/fr)
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            localizationValueService.setTranslation(localizationId, entry.getKey(), entry.getValue());
        }

        // Create sample type
        TypeOfSample sampleType = new TypeOfSample();
        sampleType.setDescription(description);
        sampleType.setLocalAbbreviation(localAbbreviation);
        sampleType.setDomain(domain);
        sampleType.setLocalization(localization);

        // Set active status
        String isActive = getValueOrEmpty(values, isActiveIndex);
        if (!isActive.isEmpty()) {
            sampleType.setActive("Y".equalsIgnoreCase(isActive) || "true".equalsIgnoreCase(isActive));
        } else {
            sampleType.setActive(true); // Default to active
        }

        // Set sort order
        String sortOrderStr = getValueOrEmpty(values, sortOrderIndex);
        if (!sortOrderStr.isEmpty()) {
            try {
                sampleType.setSortOrder(Integer.parseInt(sortOrderStr));
            } catch (NumberFormatException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "createSampleType",
                        "Invalid sortOrder value: " + sortOrderStr + ", using default: " + defaultSortOrder);
                sampleType.setSortOrder(defaultSortOrder);
            }
        } else {
            sampleType.setSortOrder(defaultSortOrder);
        }

        sampleType.setSysUserId("1"); // System user for configuration loading

        String sampleTypeId = typeOfSampleService.insert(sampleType);
        sampleType.setId(sampleTypeId);

        LogEvent.logInfo(this.getClass().getSimpleName(), "createSampleType",
                "Created new sample type: " + description + " (" + localAbbreviation + ")");

        return sampleType;
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
     * Processes localization columns and sets up translations for the sample type.
     */
    private void processLocalization(TypeOfSample sampleType, String[] values, String description,
            Map<String, Integer> localizationColumns) {

        Map<String, String> translations = buildTranslationsMap(values, description, localizationColumns);

        Localization localization = sampleType.getLocalization();
        if (localization == null) {
            // Create new localization
            localization = new Localization();
            localization.setDescription("sampleType name");
            localization.setEnglish(translations.getOrDefault("en", description));
            localization.setFrench(translations.getOrDefault("fr", translations.getOrDefault("en", description)));
            localization.setSysUserId("1");
            String localizationId = localizationService.insert(localization);
            localization.setId(localizationId);
            sampleType.setLocalization(localization);

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
