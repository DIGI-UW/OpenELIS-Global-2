package org.openelisglobal.storage.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.dao.StorageShelfDAO;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration handler for creating storage shelves from CSV files.
 *
 * This handler processes CSV files to create storage shelf entries,
 * which are shelves within storage devices.
 *
 * CSV Format: label,code,capacityLimit,active,parentDeviceCode
 * Example:
 * label,code,capacityLimit,active,parentDeviceCode
 * Shelf 1,S1,50,true,ULF01
 * Top Shelf,TOP,30,true,FRZ01
 * Shelf A,A,25,true,REF01
 */
@Component
@Transactional
public class StorageShelfConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private StorageShelfDAO storageShelfDAO;

    @Autowired
    private StorageDeviceDAO storageDeviceDAO;

    @Override
    public String getDomainName() {
        return "storage-shelves";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 120; // After storage devices (110)
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (GenericValidator.isBlankOrNull(headerLine)) {
            throw new IllegalArgumentException("Storage shelf configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);

        int labelIndex = findColumnIndex(headers, "label");
        int codeIndex = findColumnIndex(headers, "code");
        int capacityIndex = findColumnIndex(headers, "capacityLimit");
        int activeIndex = findColumnIndex(headers, "active");
        int parentDeviceCodeIndex = findColumnIndex(headers, "parentDeviceCode");

        if (labelIndex == -1) {
            throw new IllegalArgumentException("Storage shelf configuration file " + fileName +
                    " must have a 'label' column");
        }
        if (codeIndex == -1) {
            throw new IllegalArgumentException("Storage shelf configuration file " + fileName +
                    " must have a 'code' column");
        }
        if (parentDeviceCodeIndex == -1) {
            throw new IllegalArgumentException("Storage shelf configuration file " + fileName +
                    " must have a 'parentDeviceCode' column");
        }

        String line;
        int lineNumber = 1;
        int processedCount = 0;
        int skippedCount = 0;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            line = line.trim();

            // Skip empty lines and comments
            if (GenericValidator.isBlankOrNull(line) || line.startsWith("#")) {
                continue;
            }

            try {
                boolean processed = processStorageShelfLine(line, labelIndex, codeIndex, capacityIndex,
                    activeIndex, parentDeviceCodeIndex, fileName, lineNumber);
                if (processed) {
                    processedCount++;
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Storage shelf configuration processing completed for " + fileName +
                ". Processed: " + processedCount + ", Skipped: " + skippedCount);
    }

    private boolean processStorageShelfLine(String line, int labelIndex, int codeIndex, int capacityIndex,
            int activeIndex, int parentDeviceCodeIndex, String fileName, int lineNumber) {

        String[] values = parseCsvLine(line);

        String label = getValueOrEmpty(values, labelIndex);
        String code = getValueOrEmpty(values, codeIndex);
        String capacityStr = getValueOrEmpty(values, capacityIndex);
        String activeStr = getValueOrEmpty(values, activeIndex);
        String parentDeviceCode = getValueOrEmpty(values, parentDeviceCodeIndex);

        // Validate required fields
        if (GenericValidator.isBlankOrNull(label)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageShelfLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing shelf label");
            return false;
        }

        if (GenericValidator.isBlankOrNull(code)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageShelfLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing shelf code");
            return false;
        }

        if (GenericValidator.isBlankOrNull(parentDeviceCode)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageShelfLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing parent device code");
            return false;
        }

        // Parse capacity limit (optional)
        Integer capacityLimit = null;
        if (!GenericValidator.isBlankOrNull(capacityStr)) {
            try {
                capacityLimit = Integer.parseInt(capacityStr);
            } catch (NumberFormatException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageShelfLine",
                        "Invalid capacity limit '" + capacityStr + "' on line " + lineNumber + " in " + fileName);
                return false;
            }
        }

        // Parse active flag (default to true)
        Boolean active = true;
        if (!GenericValidator.isBlankOrNull(activeStr)) {
            active = Boolean.parseBoolean(activeStr.toLowerCase());
        }

        // Find parent device
        StorageDevice parentDevice = storageDeviceDAO.findByCode(parentDeviceCode);
        if (parentDevice == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageShelfLine",
                    "Skipping line " + lineNumber + " in " + fileName +
                    ": parent device with code '" + parentDeviceCode + "' not found");
            return false;
        }

        // Check if shelf with this code already exists
        StorageShelf existingShelf = storageShelfDAO.findByCode(code);
        if (existingShelf != null) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "processStorageShelfLine",
                    "Storage shelf with code '" + code + "' already exists. Skipping line " + lineNumber);
            return false;
        }

        // Create new storage shelf
        try {
            StorageShelf shelf = new StorageShelf();
            shelf.setFhirUuid(UUID.randomUUID());
            shelf.setLabel(label);
            shelf.setCode(code);
            shelf.setCapacityLimit(capacityLimit);
            shelf.setActive(active);
            shelf.setParentDevice(parentDevice);
            shelf.setSysUserId("1"); // System user

            storageShelfDAO.insert(shelf);

            LogEvent.logInfo(this.getClass().getSimpleName(), "processStorageShelfLine",
                    "Successfully created storage shelf '" + label + "' with code '" + code +
                    "' in device '" + parentDevice.getName() + "'");
            return true;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "processStorageShelfLine",
                    "Failed to create storage shelf '" + label + "': " + e.getMessage());
            return false;
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

    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (columnName.equalsIgnoreCase(headers[i])) {
                return i;
            }
        }
        return -1;
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value : "";
        }
        return "";
    }
}