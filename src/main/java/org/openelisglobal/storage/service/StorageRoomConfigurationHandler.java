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
import org.openelisglobal.storage.dao.StorageRoomDAO;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration handler for creating storage rooms from CSV files.
 * <p>
 * This handler processes CSV files to create storage room entries, which are
 * the top-level physical locations in the storage hierarchy.
 * <p>
 * CSV Format: name,code,description,active Example:
 * name,code,description,active Main Laboratory,MAIN-LAB,Primary laboratory
 * storage facility with controlled temperature zones,true Sample Processing
 * Room,PROC-ROOM,Room dedicated to sample processing and aliquoting,true
 * Archive Storage,ARCHIVE,Long-term sample archival storage,true
 */
@Component
@Transactional
public class StorageRoomConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private StorageRoomDAO storageRoomDAO;

    @Override
    public String getDomainName() {
        return "storage-rooms";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 100; // First in storage hierarchy
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (GenericValidator.isBlankOrNull(headerLine)) {
            throw new IllegalArgumentException("Storage room configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);

        int nameIndex = findColumnIndex(headers, "name");
        int codeIndex = findColumnIndex(headers, "code");
        int descriptionIndex = findColumnIndex(headers, "description");
        int activeIndex = findColumnIndex(headers, "active");

        if (nameIndex == -1) {
            throw new IllegalArgumentException(
                    "Storage room configuration file " + fileName + " must have a 'name' column");
        }
        if (codeIndex == -1) {
            throw new IllegalArgumentException(
                    "Storage room configuration file " + fileName + " must have a 'code' column");
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
                boolean processed = processStorageRoomLine(line, nameIndex, codeIndex, descriptionIndex, activeIndex,
                        fileName, lineNumber);
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
                "Storage room configuration processing completed for " + fileName + ". Processed: " + processedCount
                        + ", Skipped: " + skippedCount);
    }

    private boolean processStorageRoomLine(String line, int nameIndex, int codeIndex, int descriptionIndex,
            int activeIndex, String fileName, int lineNumber) {

        String[] values = parseCsvLine(line);

        String name = getValueOrEmpty(values, nameIndex);
        String code = getValueOrEmpty(values, codeIndex);
        String description = getValueOrEmpty(values, descriptionIndex);
        String activeStr = getValueOrEmpty(values, activeIndex);

        // Validate required fields
        if (GenericValidator.isBlankOrNull(name)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageRoomLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing room name");
            return false;
        }

        if (GenericValidator.isBlankOrNull(code)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageRoomLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing room code");
            return false;
        }

        // Parse active flag (default to true)
        Boolean active = true;
        if (!GenericValidator.isBlankOrNull(activeStr)) {
            active = Boolean.parseBoolean(activeStr.toLowerCase());
        }

        // Check if room with this code already exists
        StorageRoom existingRoom = storageRoomDAO.findByCode(code);
        if (existingRoom != null) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "processStorageRoomLine",
                    "Storage room with code '" + code + "' already exists. Skipping line " + lineNumber);
            return false;
        }

        // Create new storage room
        try {
            StorageRoom room = new StorageRoom();
            room.setFhirUuid(UUID.randomUUID());
            room.setName(name);
            room.setCode(code);
            room.setDescription(description);
            room.setActive(active);
            room.setSysUserId("1"); // System user

            storageRoomDAO.insert(room);

            LogEvent.logInfo(this.getClass().getSimpleName(), "processStorageRoomLine",
                    "Successfully created storage room '" + name + "' with code '" + code + "'");
            return true;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "processStorageRoomLine",
                    "Failed to create storage room '" + name + "': " + e.getMessage());
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