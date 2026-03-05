package org.openelisglobal.dictionary.service;

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
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationValueService;
import org.openelisglobal.localization.service.SupportedLocaleService;
import org.openelisglobal.localization.valueholder.Localization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handler for loading dictionary configuration files. Supports CSV format with
 * dictionary entries and their categories.
 *
 * Expected CSV format:
 * category,dictEntry,localAbbreviation,isActive,sortOrder,loincCode,localization:en,localization:fr
 * Sample Types,Blood,BLD,Y,1,26881-3,Blood,Sang Sample
 * Types,Serum,SER,Y,2,26882-1,Serum,Sérum
 *
 * Notes: - First line is the header (required) - category and dictEntry are
 * required fields - localAbbreviation, isActive, sortOrder, loincCode are
 * optional - isActive defaults to "Y" if not specified - localization:xx
 * columns (where xx is a locale code like en, fr, es) provide translations - If
 * no localization columns are provided, dictEntry is used as the default value
 * for the fallback locale
 */
@Component
public class DictionaryConfigurationHandler implements DomainConfigurationHandler {

    private static final String LOCALIZATION_COLUMN_PREFIX = "localization:";

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private LocalizationValueService localizationValueService;

    @Autowired
    private SupportedLocaleService supportedLocaleService;

    @Override
    public String getDomainName() {
        return "dictionaries";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 300; // Independent higher-level configuration
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Dictionary configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        validateHeaders(headers, fileName);

        int categoryIndex = findColumnIndex(headers, "category");
        int dictEntryIndex = findColumnIndex(headers, "dictEntry");
        int localAbbreviationIndex = findColumnIndex(headers, "localAbbreviation");
        int isActiveIndex = findColumnIndex(headers, "isActive");
        int sortOrderIndex = findColumnIndex(headers, "sortOrder");
        int loincCodeIndex = findColumnIndex(headers, "loincCode");

        // Detect localization columns (localization:en, localization:fr, etc.)
        Map<String, Integer> localizationColumns = detectLocalizationColumns(headers);

        List<Dictionary> processedDictionaries = new ArrayList<>();
        String line;
        int lineNumber = 1; // Start at 1 since we already read the header

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            // Skip empty lines and comments (lines starting with #)
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            try {
                String[] values = parseCsvLine(line);
                Dictionary dictionary = processCsvLine(values, categoryIndex, dictEntryIndex, localAbbreviationIndex,
                        isActiveIndex, sortOrderIndex, loincCodeIndex, localizationColumns);
                if (dictionary != null) {
                    processedDictionaries.add(dictionary);
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }

        DisplayListService.getInstance().refreshLists();

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Successfully loaded " + processedDictionaries.size() + " dictionaries from " + fileName);
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
        boolean hasCategoryColumn = false;
        boolean hasDictEntryColumn = false;

        for (String header : headers) {
            if ("category".equalsIgnoreCase(header)) {
                hasCategoryColumn = true;
            }
            if ("dictEntry".equalsIgnoreCase(header)) {
                hasDictEntryColumn = true;
            }
        }

        if (!hasCategoryColumn) {
            throw new IllegalArgumentException(
                    "Dictionary configuration file " + fileName + " must have a 'category' column");
        }
        if (!hasDictEntryColumn) {
            throw new IllegalArgumentException(
                    "Dictionary configuration file " + fileName + " must have a 'dictEntry' column");
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

    private Dictionary processCsvLine(String[] values, int categoryIndex, int dictEntryIndex,
            int localAbbreviationIndex, int isActiveIndex, int sortOrderIndex, int loincCodeIndex,
            Map<String, Integer> localizationColumns) {

        String categoryName = getValueOrEmpty(values, categoryIndex);
        String dictEntry = getValueOrEmpty(values, dictEntryIndex);

        if (categoryName.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine", "Skipping row with missing category");
            return null;
        }

        if (dictEntry.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine", "Skipping row with missing dictEntry");
            return null;
        }

        DictionaryCategory category = getOrCreateCategory(categoryName);

        // The same entry name CAN exist in different categories
        Dictionary existingDict = dictionaryService.getDictionaryEntrysByNameAndCategoryDescription(dictEntry,
                categoryName);
        if (existingDict != null) {
            updateDictionaryFromCsv(existingDict, values, category, localAbbreviationIndex, isActiveIndex,
                    sortOrderIndex, loincCodeIndex, dictEntry, localizationColumns);
            dictionaryService.update(existingDict);
            return existingDict;
        } else {
            Dictionary newDict = new Dictionary();
            newDict.setDictEntry(dictEntry);
            updateDictionaryFromCsv(newDict, values, category, localAbbreviationIndex, isActiveIndex, sortOrderIndex,
                    loincCodeIndex, dictEntry, localizationColumns);
            String dictId = dictionaryService.insert(newDict);
            newDict.setId(dictId);
            return newDict;
        }
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value : "";
        }
        return "";
    }

    private DictionaryCategory getOrCreateCategory(String categoryName) {
        DictionaryCategory category = dictionaryCategoryService.getDictionaryCategoryByName(categoryName);

        if (category == null) {
            category = tryCreateCategoryWithUniqueAbbreviation(categoryName);
        }

        return category;
    }

    private DictionaryCategory tryCreateCategoryWithUniqueAbbreviation(String categoryName) {
        String baseAbbreviation = categoryName.replaceAll("\\s+", "").toUpperCase();
        if (baseAbbreviation.length() > 5) {
            baseAbbreviation = baseAbbreviation.substring(0, 5);
        }

        String abbreviation = baseAbbreviation;
        int suffix = 1;

        while (suffix <= 99) {
            try {
                DictionaryCategory category = new DictionaryCategory();
                category.setCategoryName(categoryName);
                // Use category name as description to avoid duplicate description conflicts
                category.setDescription(categoryName);
                category.setLocalAbbreviation(abbreviation);

                String categoryId = dictionaryCategoryService.insert(category);
                category = dictionaryCategoryService.get(categoryId);
                LogEvent.logInfo(this.getClass().getSimpleName(), "tryCreateCategoryWithUniqueAbbreviation",
                        "Created new dictionary category: " + categoryName + " with abbreviation: " + abbreviation);
                return category;

            } catch (org.openelisglobal.common.exception.LIMSDuplicateRecordException e) {
                // Duplicate found, try with a suffix
                LogEvent.logDebug(this.getClass().getSimpleName(), "tryCreateCategoryWithUniqueAbbreviation",
                        "Abbreviation " + abbreviation + " already exists, trying with suffix " + suffix);

                int maxBaseLength = 5 - String.valueOf(suffix).length();
                if (maxBaseLength < 1) {
                    maxBaseLength = 1;
                }
                String truncatedBase = baseAbbreviation.length() > maxBaseLength
                        ? baseAbbreviation.substring(0, maxBaseLength)
                        : baseAbbreviation;
                abbreviation = truncatedBase + suffix;
                suffix++;
            }
        }

        // If we exhausted all attempts, throw an exception
        throw new IllegalStateException(
                "Could not create dictionary category '" + categoryName + "' - exhausted all abbreviation attempts");
    }

    private void updateDictionaryFromCsv(Dictionary dictionary, String[] values, DictionaryCategory category,
            int localAbbreviationIndex, int isActiveIndex, int sortOrderIndex, int loincCodeIndex, String dictEntry,
            Map<String, Integer> localizationColumns) {

        dictionary.setDictionaryCategory(category);

        String localAbbreviation = getValueOrEmpty(values, localAbbreviationIndex);
        if (!localAbbreviation.isEmpty()) {
            dictionary.setLocalAbbreviation(localAbbreviation);
        }

        String isActive = getValueOrEmpty(values, isActiveIndex);
        if (!isActive.isEmpty()) {
            dictionary.setIsActive(isActive);
        } else {
            dictionary.setIsActive("Y"); // Default to active
        }

        String sortOrderStr = getValueOrEmpty(values, sortOrderIndex);
        if (!sortOrderStr.isEmpty()) {
            try {
                dictionary.setSortOrder(Integer.parseInt(sortOrderStr));
            } catch (NumberFormatException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "updateDictionaryFromCsv",
                        "Invalid sortOrder value: " + sortOrderStr);
            }
        }

        String loincCode = getValueOrEmpty(values, loincCodeIndex);
        if (!loincCode.isEmpty()) {
            dictionary.setLoincCode(loincCode);
        }

        // Set system user ID for audit
        dictionary.setSysUserId("1"); // System user for configuration loading

        // Handle localization
        processLocalization(dictionary, values, dictEntry, localizationColumns);
    }

    /**
     * Processes localization columns and sets up translations for the dictionary
     * entry. If no localization columns are provided, uses dictEntry as the default
     * value for the fallback locale (en).
     *
     * @param dictionary          the Dictionary entity to update
     * @param values              the CSV row values
     * @param dictEntry           the dictionary entry name (used as default if no
     *                            translations)
     * @param localizationColumns map of locale code to column index
     */
    private void processLocalization(Dictionary dictionary, String[] values, String dictEntry,
            Map<String, Integer> localizationColumns) {

        // Get or create localization for this dictionary
        Localization localization = dictionary.getLocalizedDictionaryName();
        boolean isNewLocalization = false;

        if (localization == null) {
            localization = new Localization();
            localization.setDescription("dictionary entry: " + dictEntry);
            localization.setSysUserId("1");
            isNewLocalization = true;
        }

        // Determine what translations to set
        Map<String, String> translations = new HashMap<>();

        if (localizationColumns.isEmpty()) {
            // No localization columns provided - use dictEntry as the fallback (en) value
            translations.put("en", dictEntry);
        } else {
            // Process each localization column
            for (Map.Entry<String, Integer> entry : localizationColumns.entrySet()) {
                String locale = entry.getKey();
                String translationValue = getValueOrEmpty(values, entry.getValue());
                if (!translationValue.isEmpty()) {
                    translations.put(locale, translationValue);
                }
            }

            // If no valid translations found, use dictEntry as fallback
            if (translations.isEmpty()) {
                translations.put("en", dictEntry);
            }
        }

        if (isNewLocalization) {
            // For new localization, set initial values using the deprecated setters
            // which handle both legacy and new systems
            for (Map.Entry<String, String> entry : translations.entrySet()) {
                if ("en".equals(entry.getKey())) {
                    localization.setEnglish(entry.getValue());
                } else if ("fr".equals(entry.getKey())) {
                    localization.setFrench(entry.getValue());
                }
            }

            // Insert the localization to get an ID
            String localizationId = localizationService.insert(localization);
            localization.setId(localizationId);
            dictionary.setLocalizedDictionaryName(localization);

            // Now set all translations using the service (including any beyond en/fr)
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
