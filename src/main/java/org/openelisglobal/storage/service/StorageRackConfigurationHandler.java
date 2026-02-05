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
import org.openelisglobal.storage.dao.StorageRackDAO;
import org.openelisglobal.storage.dao.StorageShelfDAO;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration handler for creating storage racks from CSV files.
 *
 * This handler processes CSV files to create storage rack entries, which are
 * racks within storage shelves.
 *
 * CSV Format: label,code,active,parentShelfCode Example:
 * label,code,active,parentShelfCode Rack 1,R1,true,S1 Tray A,TA,true,TOP
 * Position 1,P1,true,A
 */
@Component
@Transactional
public class StorageRackConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private StorageRackDAO storageRackDAO;

    @Autowired
    private StorageShelfDAO storageShelfDAO;

    @Override
    public String getDomainName() {
        return "storage-racks";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 130; // After storage shelves (120)
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (GenericValidator.isBlankOrNull(headerLine)) {
            throw new IllegalArgumentException("Storage rack configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);

        int labelIndex = findColumnIndex(headers, "label");
        int codeIndex = findColumnIndex(headers, "code");
        int activeIndex = findColumnIndex(headers, "active");
        int parentShelfCodeIndex = findColumnIndex(headers, "parentShelfCode");

        if (labelIndex == -1) {
            throw new IllegalArgumentException(
                    "Storage rack configuration file " + fileName + " must have a 'label' column");
        }
        if (codeIndex == -1) {
            throw new IllegalArgumentException(
                    "Storage rack configuration file " + fileName + " must have a 'code' column");
        }
        if (parentShelfCodeIndex == -1) {
            throw new IllegalArgumentException(
                    "Storage rack configuration file " + fileName + " must have a 'parentShelfCode' column");
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
                boolean processed = processStorageRackLine(line, labelIndex, codeIndex, activeIndex,
                        parentShelfCodeIndex, fileName, lineNumber);
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
                "Storage rack configuration processing completed for " + fileName + ". Processed: " + processedCount
                        + ", Skipped: " + skippedCount);
    }

    private boolean processStorageRackLine(String line, int labelIndex, int codeIndex, int activeIndex,
            int parentShelfCodeIndex, String fileName, int lineNumber) {

        String[] values = parseCsvLine(line);

        String label = getValueOrEmpty(values, labelIndex);
        String code = getValueOrEmpty(values, codeIndex);
        String activeStr = getValueOrEmpty(values, activeIndex);
        String parentShelfCode = getValueOrEmpty(values, parentShelfCodeIndex);

        // Validate required fields
        if (GenericValidator.isBlankOrNull(label)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageRackLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing rack label");
            return false;
        }

        if (GenericValidator.isBlankOrNull(code)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageRackLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing rack code");
            return false;
        }

        if (GenericValidator.isBlankOrNull(parentShelfCode)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageRackLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing parent shelf code");
            return false;
        }

        // Parse active flag (default to true)
        Boolean active = true;
        if (!GenericValidator.isBlankOrNull(activeStr)) {
            active = Boolean.parseBoolean(activeStr.toLowerCase());
        }

        // Find parent shelf
        StorageShelf parentShelf = storageShelfDAO.findByCode(parentShelfCode);
        if (parentShelf == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageRackLine", "Skipping line " + lineNumber
                    + " in " + fileName + ": parent shelf with code '" + parentShelfCode + "' not found");
            return false;
        }

        // Check if rack with this code already exists
        StorageRack existingRack = storageRackDAO.findByCode(code);
        if (existingRack != null) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "processStorageRackLine",
                    "Storage rack with code '" + code + "' already exists. Skipping line " + lineNumber);
            return false;
        }

        // Create new storage rack
        try {
            StorageRack rack = new StorageRack();
            rack.setFhirUuid(UUID.randomUUID());
            rack.setLabel(label);
            rack.setCode(code);
            rack.setActive(active);
            rack.setParentShelf(parentShelf);
            rack.setSysUserId("1"); // System user

            storageRackDAO.insert(rack);

            LogEvent.logInfo(this.getClass().getSimpleName(), "processStorageRackLine",
                    "Successfully created storage rack '" + label + "' with code '" + code + "' in shelf '"
                            + parentShelf.getLabel() + "'");
            return true;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "processStorageRackLine",
                    "Failed to create storage rack '" + label + "': " + e.getMessage());
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