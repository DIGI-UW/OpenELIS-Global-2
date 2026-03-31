package org.openelisglobal.qaevent.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.qaevent.valueholder.NceCategory;
import org.openelisglobal.qaevent.valueholder.NceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Configuration handler for NCE (Non-Conforming Event) types. Loads NCE types
 * from CSV into the nce_type table with localization support.
 *
 * Links each NceType to its corresponding NceCategory.
 *
 * Expected CSV format:
 * name,displayKey,categoryName,active,localization:en,localization:fr Sample
 * collection error,nce.type.sampleCollectionError,Sample,Y,Sample collection
 * error,Erreur de prélèvement
 *
 * This handler runs AFTER the nce-categories handler (loadOrder 320 > 310) so
 * that nce_category records already exist for linking.
 */
@Component
public class NceTypeConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private NceTypeService nceTypeService;

    @Autowired
    private NceCategoryService nceCategoryService;

    @Autowired
    private LocalizationService localizationService;

    @Override
    public String getDomainName() {
        return "nce-types";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 320; // After nce-categories (310)
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("NCE type configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        validateHeaders(headers, fileName);

        int nameIndex = findColumnIndex(headers, "name");
        int displayKeyIndex = findColumnIndex(headers, "displayKey");
        int categoryNameIndex = findColumnIndex(headers, "categoryName");
        int activeIndex = findColumnIndex(headers, "active");

        // Find localization columns (localization:en, localization:fr, etc.)
        Map<String, Integer> localeColumnIndexes = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim();
            if (header.startsWith("localization:")) {
                String locale = header.substring("localization:".length());
                localeColumnIndexes.put(locale, i);
            }
        }

        int processedCount = 0;
        String line;
        int lineNumber = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            try {
                String[] values = parseCsvLine(line);
                boolean processed = processCsvLine(values, nameIndex, displayKeyIndex, categoryNameIndex, activeIndex,
                        localeColumnIndexes);
                if (processed) {
                    processedCount++;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Successfully loaded " + processedCount + " NCE types from " + fileName);
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
        if (findColumnIndex(headers, "name") < 0) {
            throw new IllegalArgumentException(
                    "NCE type configuration file " + fileName + " must have a 'name' column");
        }
    }

    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (columnName.equalsIgnoreCase(headers[i].trim())) {
                return i;
            }
        }
        return -1;
    }

    private boolean processCsvLine(String[] values, int nameIndex, int displayKeyIndex, int categoryNameIndex,
            int activeIndex, Map<String, Integer> localeColumnIndexes) {
        String name = getValueOrEmpty(values, nameIndex);
        String displayKey = getValueOrEmpty(values, displayKeyIndex);
        String categoryName = getValueOrEmpty(values, categoryNameIndex);
        String active = getValueOrEmpty(values, activeIndex);

        if (name.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine", "Skipping row with missing name");
            return false;
        }

        // Find the corresponding nce_category
        NceCategory category = findCategoryByName(categoryName);
        Integer categoryId = category != null ? Integer.valueOf(category.getId()) : null;

        // Check if type already exists by name
        NceType existing = findTypeByName(name);

        if (existing != null) {
            // Update existing type
            updateType(existing, displayKey, active, categoryId, values, localeColumnIndexes);
            nceTypeService.update(existing);
            LogEvent.logDebug(this.getClass().getSimpleName(), "processCsvLine", "Updated NCE type: " + name);
        } else {
            // Create new NCE type with localization
            Localization localization = createLocalization(name, values, localeColumnIndexes);

            NceType type = new NceType();
            type.setName(name);
            type.setDisplayKey(displayKey.isEmpty() ? null : displayKey);
            type.setActive(active.isEmpty() || "Y".equalsIgnoreCase(active));
            type.setCategoryId(categoryId);
            type.setNameLocalization(localization);
            type.setSysUserId("1");

            nceTypeService.insert(type);

            if (categoryId != null) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "processCsvLine",
                        "Created NCE type: " + name + " linked to category: " + categoryName);
            } else {
                LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                        "Created NCE type: " + name + " but no matching category found for: " + categoryName);
            }
        }

        return true;
    }

    private NceType findTypeByName(String name) {
        List<NceType> all = nceTypeService.getAllNceTypes();
        for (NceType type : all) {
            if (name.equals(type.getName())) {
                return type;
            }
        }
        return null;
    }

    private NceCategory findCategoryByName(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) {
            return null;
        }
        List<NceCategory> all = nceCategoryService.getAllNceCategories();
        for (NceCategory cat : all) {
            if (categoryName.equals(cat.getName())) {
                return cat;
            }
        }
        return null;
    }

    private void updateType(NceType type, String displayKey, String active, Integer categoryId, String[] values,
            Map<String, Integer> localeColumnIndexes) {
        if (!displayKey.isEmpty()) {
            type.setDisplayKey(displayKey);
        }
        if (!active.isEmpty()) {
            type.setActive("Y".equalsIgnoreCase(active));
        }
        if (categoryId != null) {
            type.setCategoryId(categoryId);
        }

        // Update or create localization
        Localization localization = type.getNameLocalization();
        if (localization == null) {
            localization = createLocalization(type.getName(), values, localeColumnIndexes);
            type.setNameLocalization(localization);
        } else {
            updateLocalization(localization, values, localeColumnIndexes);
            localizationService.update(localization);
        }

        type.setSysUserId("1");
    }

    private Localization createLocalization(String description, String[] values,
            Map<String, Integer> localeColumnIndexes) {
        Localization localization = new Localization();
        localization.setDescription("NCE Type: " + description);
        localization.setSysUserId("1");

        for (Map.Entry<String, Integer> entry : localeColumnIndexes.entrySet()) {
            String locale = entry.getKey();
            String value = getValueOrEmpty(values, entry.getValue());
            if (!value.isEmpty()) {
                localization.setLocalizedValue(locale, value);
            }
        }

        localizationService.insert(localization);
        return localization;
    }

    private void updateLocalization(Localization localization, String[] values,
            Map<String, Integer> localeColumnIndexes) {
        for (Map.Entry<String, Integer> entry : localeColumnIndexes.entrySet()) {
            String locale = entry.getKey();
            String value = getValueOrEmpty(values, entry.getValue());
            if (!value.isEmpty()) {
                localization.setLocalizedValue(locale, value);
            }
        }
        localization.setSysUserId("1");
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value.trim() : "";
        }
        return "";
    }
}
