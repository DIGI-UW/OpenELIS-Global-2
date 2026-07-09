package org.openelisglobal.inventory.service;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-658 Part A — config-driven starter set of inventory item types, on top of
 * the 5 codes seeded directly by {@code 070-inventory-item-type.xml}
 * (REAGENT/RDT/CARTRIDGE/HIV_KIT/SYPHILIS_KIT). Lets a deployment ship a richer
 * starting list by editing a CSV, without a Liquibase migration or app redeploy
 * — admins can still add/rename/deactivate types at runtime via the Inventory
 * Item Types admin page regardless; this handler only affects what a fresh
 * install starts with.
 *
 * <p>
 * Expected CSV format:
 * {@code code,sortOrder,active,localization:en,localization:fr,localization:id}
 * (arbitrary column order, any subset of locale columns — mirrors
 * {@code DictionaryConfigurationHandler}/{@code NceCategoryConfigurationHandler}).
 * Upserts by {@code code}, so re-running with an edited CSV updates existing
 * rows in place rather than duplicating them.
 */
@Component
public class InventoryItemTypeConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private InventoryItemTypeService inventoryItemTypeService;

    @Override
    public String getDomainName() {
        return "inventory-item-types";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 320;
    }

    @Override
    @Transactional
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Inventory item type configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        int codeIndex = findColumnIndex(headers, "code");
        int sortOrderIndex = findColumnIndex(headers, "sortOrder");
        int activeIndex = findColumnIndex(headers, "active");
        if (codeIndex < 0) {
            throw new IllegalArgumentException(
                    "Inventory item type configuration file " + fileName + " must have a 'code' column");
        }

        Map<String, Integer> localeColumnIndexes = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim();
            if (header.startsWith("localization:")) {
                localeColumnIndexes.put(header.substring("localization:".length()), i);
            }
        }

        String line;
        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
            try {
                String[] values = parseCsvLine(line);
                processCsvLine(values, codeIndex, sortOrderIndex, activeIndex, localeColumnIndexes);
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }
    }

    private void processCsvLine(String[] values, int codeIndex, int sortOrderIndex, int activeIndex,
            Map<String, Integer> localeColumnIndexes) {
        String code = getValueOrEmpty(values, codeIndex);
        if (code.isEmpty()) {
            return;
        }
        String sortOrderText = getValueOrEmpty(values, sortOrderIndex);
        String activeText = getValueOrEmpty(values, activeIndex);
        Integer sortOrder = sortOrderText.isEmpty() ? null : Integer.valueOf(sortOrderText);
        boolean active = activeText.isEmpty() || "Y".equalsIgnoreCase(activeText);

        Map<String, String> localizedNames = new HashMap<>();
        for (Map.Entry<String, Integer> entry : localeColumnIndexes.entrySet()) {
            String value = getValueOrEmpty(values, entry.getValue());
            if (!value.isEmpty()) {
                localizedNames.put(entry.getKey(), value);
            }
        }
        if (localizedNames.isEmpty()) {
            return;
        }

        inventoryItemTypeService.upsertSeeded(code, sortOrder, active, localizedNames, "1");
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

    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (columnName.equalsIgnoreCase(headers[i].trim())) {
                return i;
            }
        }
        return -1;
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value.trim() : "";
        }
        return "";
    }
}
