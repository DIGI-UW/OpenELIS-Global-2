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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration handler for NCE (Non-Conforming Event) categories. Loads NCE
 * categories from CSV into the nce_category table with localization support.
 *
 * Expected CSV format: name,displayKey,active,localization:en,localization:fr
 * General,nce.category.general,Y,General,Général
 *
 * This handler runs after dictionaries (loadOrder 310 > 300).
 */
@Component
public class NceCategoryConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private NceCategoryService nceCategoryService;

    @Autowired
    private LocalizationService localizationService;

    @Override
    public String getDomainName() {
        return "nce-categories";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 310;
    }

    @Override
    @Transactional
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("NCE category configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        validateHeaders(headers, fileName);

        int nameIndex = findColumnIndex(headers, "name");
        int displayKeyIndex = findColumnIndex(headers, "displayKey");
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

        // Load all existing categories into a map for O(1) lookups
        Map<String, NceCategory> existingByName = new HashMap<>();
        for (NceCategory cat : nceCategoryService.getAllNceCategories()) {
            if (cat.getName() != null) {
                existingByName.put(cat.getName(), cat);
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
                boolean processed = processCsvLine(values, nameIndex, displayKeyIndex, activeIndex,
                        localeColumnIndexes, existingByName);
                if (processed) {
                    processedCount++;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }

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
                    "NCE category configuration file " + fileName + " must have a 'name' column");
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

    private boolean processCsvLine(String[] values, int nameIndex, int displayKeyIndex, int activeIndex,
            Map<String, Integer> localeColumnIndexes, Map<String, NceCategory> existingByName) {
        String name = getValueOrEmpty(values, nameIndex);
        String displayKey = getValueOrEmpty(values, displayKeyIndex);
        String active = getValueOrEmpty(values, activeIndex);

        if (name.isEmpty()) {
            return false;
        }

        // Check if category already exists by name (O(1) lookup)
        NceCategory existing = existingByName.get(name);

        if (existing != null) {
            // Update existing category
            updateCategory(existing, displayKey, active, values, localeColumnIndexes);
            nceCategoryService.update(existing);
        } else {
            // Create new NCE category with localization
            Localization localization = createLocalization(name, values, localeColumnIndexes);

            NceCategory category = new NceCategory();
            category.setName(name);
            category.setDisplayKey(displayKey.isEmpty() ? null : displayKey);
            category.setActive(active.isEmpty() || "Y".equalsIgnoreCase(active));
            category.setNameLocalization(localization);
            category.setSysUserId("1");

            nceCategoryService.insert(category);

            // Add to map for potential subsequent lookups within same import
            existingByName.put(name, category);
        }

        return true;
    }

    private void updateCategory(NceCategory category, String displayKey, String active, String[] values,
            Map<String, Integer> localeColumnIndexes) {
        if (!displayKey.isEmpty()) {
            category.setDisplayKey(displayKey);
        }
        if (!active.isEmpty()) {
            category.setActive("Y".equalsIgnoreCase(active));
        }

        // Update or create localization
        Localization localization = category.getNameLocalization();
        if (localization == null) {
            localization = createLocalization(category.getName(), values, localeColumnIndexes);
            category.setNameLocalization(localization);
        } else {
            updateLocalization(localization, values, localeColumnIndexes);
            localizationService.update(localization);
        }

        category.setSysUserId("1");
    }

    private Localization createLocalization(String description, String[] values,
            Map<String, Integer> localeColumnIndexes) {
        Localization localization = new Localization();
        localization.setDescription("NCE Category: " + description);
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
