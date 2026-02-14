package org.openelisglobal.dictionary.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handler for loading dictionary configuration files. Supports CSV format with
 * dictionary entries and their categories.
 *
 * Expected CSV format:
 * category,dictEntry,localAbbreviation,isActive,sortOrder,loincCode Sample
 * Types,Blood,BLD,Y,1,26881-3 Sample Types,Serum,SER,Y,2,26882-1
 *
 * Notes: - First line is the header (required) - category and dictEntry are
 * required fields - localAbbreviation, isActive, sortOrder, loincCode are
 * optional - isActive defaults to "Y" if not specified
 */
@Component
public class DictionaryConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

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

        // Read and validate header
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Dictionary configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        validateHeaders(headers, fileName);

        // Get column indices
        int categoryIndex = findColumnIndex(headers, "category");
        int dictEntryIndex = findColumnIndex(headers, "dictEntry");
        int localAbbreviationIndex = findColumnIndex(headers, "localAbbreviation");
        int isActiveIndex = findColumnIndex(headers, "isActive");
        int sortOrderIndex = findColumnIndex(headers, "sortOrder");
        int loincCodeIndex = findColumnIndex(headers, "loincCode");

        List<Dictionary> processedDictionaries = new ArrayList<>();
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
                Dictionary dictionary = processCsvLine(values, categoryIndex, dictEntryIndex, localAbbreviationIndex,
                        isActiveIndex, sortOrderIndex, loincCodeIndex);
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
            int localAbbreviationIndex, int isActiveIndex, int sortOrderIndex, int loincCodeIndex) {

        // Get required fields
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

        // Get or create category
        DictionaryCategory category = getOrCreateCategory(categoryName);

        // Check if dictionary already exists in this category
        Dictionary existingDict = dictionaryService.getDictionaryByDictEntry(dictEntry);
        if (existingDict != null && existingDict.getDictionaryCategory() != null
                && existingDict.getDictionaryCategory().getId().equals(category.getId())) {
            // Update existing dictionary
            updateDictionaryFromCsv(existingDict, values, category, localAbbreviationIndex, isActiveIndex,
                    sortOrderIndex, loincCodeIndex);
            dictionaryService.update(existingDict);
            return existingDict;
        } else if (existingDict != null) {
            // Dictionary exists but in different category, skip
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Dictionary entry '" + dictEntry + "' already exists in a different category. Skipping.");
            return null;
        } else {
            // Create new dictionary
            Dictionary newDict = new Dictionary();
            newDict.setDictEntry(dictEntry);
            updateDictionaryFromCsv(newDict, values, category, localAbbreviationIndex, isActiveIndex, sortOrderIndex,
                    loincCodeIndex);
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
        // Try to find existing category by name
        DictionaryCategory category = dictionaryCategoryService.getDictionaryCategoryByName(categoryName);

        if (category == null) {
            // Generate unique abbreviation and try to create category
            category = tryCreateCategoryWithUniqueAbbreviation(categoryName);
        }

        return category;
    }

    private DictionaryCategory tryCreateCategoryWithUniqueAbbreviation(String categoryName) {
        // Generate base abbreviation from category name (first 3-5 chars, uppercase)
        String baseAbbreviation = categoryName.replaceAll("\\s+", "").toUpperCase();
        if (baseAbbreviation.length() > 5) {
            baseAbbreviation = baseAbbreviation.substring(0, 5);
        }

        String abbreviation = baseAbbreviation;
        int suffix = 1;

        // Keep trying to create the category with different abbreviations until
        // successful
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

                // Truncate base to make room for suffix
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
            int localAbbreviationIndex, int isActiveIndex, int sortOrderIndex, int loincCodeIndex) {

        dictionary.setDictionaryCategory(category);

        // Set optional fields
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
    }
}
