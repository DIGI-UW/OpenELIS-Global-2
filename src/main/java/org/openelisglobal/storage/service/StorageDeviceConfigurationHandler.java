package org.openelisglobal.storage.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.dao.StorageRoomDAO;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration handler for creating storage devices from CSV files.
 * <p>
 * This handler processes CSV files to create storage device entries,
 * which are storage equipment (freezers, refrigerators, cabinets) within rooms.
 * <p>
 * CSV Format: name,code,type,temperatureSetting,capacityLimit,active,parentRoomCode
 * Example:
 * name,code,type,temperatureSetting,capacityLimit,active,parentRoomCode
 * Ultra-Low Freezer 1,ULF01,freezer,-80.0,1000,true,MAIN-LAB
 * Refrigerator 1,REF01,refrigerator,4.0,300,true,MAIN-LAB
 * Processing Cabinet,PCAB01,cabinet,,100,true,PROC-ROOM
 */
@Component
@Transactional
public class StorageDeviceConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private StorageDeviceDAO storageDeviceDAO;

    @Autowired
    private StorageRoomDAO storageRoomDAO;

    @Override
    public String getDomainName() {
        return "storage-devices";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 110; // After storage rooms (100)
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (GenericValidator.isBlankOrNull(headerLine)) {
            throw new IllegalArgumentException("Storage device configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);

        int nameIndex = findColumnIndex(headers, "name");
        int codeIndex = findColumnIndex(headers, "code");
        int typeIndex = findColumnIndex(headers, "type");
        int temperatureIndex = findColumnIndex(headers, "temperatureSetting");
        int capacityIndex = findColumnIndex(headers, "capacityLimit");
        int activeIndex = findColumnIndex(headers, "active");
        int parentRoomCodeIndex = findColumnIndex(headers, "parentRoomCode");

        if (nameIndex == -1) {
            throw new IllegalArgumentException("Storage device configuration file " + fileName +
                    " must have a 'name' column");
        }
        if (codeIndex == -1) {
            throw new IllegalArgumentException("Storage device configuration file " + fileName +
                    " must have a 'code' column");
        }
        if (typeIndex == -1) {
            throw new IllegalArgumentException("Storage device configuration file " + fileName +
                    " must have a 'type' column");
        }
        if (parentRoomCodeIndex == -1) {
            throw new IllegalArgumentException("Storage device configuration file " + fileName +
                    " must have a 'parentRoomCode' column");
        }

        String line;
        int lineNumber = 1;
        int processedCount = 0;
        int skippedCount = 0;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            line = line.trim();

            if (GenericValidator.isBlankOrNull(line) || line.startsWith("#")) {
                continue;
            }

            try {
                boolean processed = processStorageDeviceLine(line, nameIndex, codeIndex, typeIndex,
                    temperatureIndex, capacityIndex, activeIndex, parentRoomCodeIndex, fileName, lineNumber);
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
                "Storage device configuration processing completed for " + fileName +
                ". Processed: " + processedCount + ", Skipped: " + skippedCount);
    }

    private boolean processStorageDeviceLine(String line, int nameIndex, int codeIndex, int typeIndex,
            int temperatureIndex, int capacityIndex, int activeIndex, int parentRoomCodeIndex, String fileName, int lineNumber) {

        String[] values = parseCsvLine(line);

        String name = getValueOrEmpty(values, nameIndex);
        String code = getValueOrEmpty(values, codeIndex);
        String type = getValueOrEmpty(values, typeIndex);
        String temperatureStr = getValueOrEmpty(values, temperatureIndex);
        String capacityStr = getValueOrEmpty(values, capacityIndex);
        String activeStr = getValueOrEmpty(values, activeIndex);
        String parentRoomCode = getValueOrEmpty(values, parentRoomCodeIndex);

        if (GenericValidator.isBlankOrNull(name)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageDeviceLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing device name");
            return false;
        }

        if (GenericValidator.isBlankOrNull(code)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageDeviceLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing device code");
            return false;
        }

        if (GenericValidator.isBlankOrNull(type)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageDeviceLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing device type");
            return false;
        }

        if (GenericValidator.isBlankOrNull(parentRoomCode)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageDeviceLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing parent room code");
            return false;
        }

        StorageDevice.DeviceType deviceType;
        try {
            deviceType = StorageDevice.DeviceType.fromValue(type.toLowerCase());
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageDeviceLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": invalid device type '" + type +
                    "'. Valid types are: freezer, refrigerator, cabinet, other");
            return false;
        }

        BigDecimal temperatureSetting = null;
        if (!GenericValidator.isBlankOrNull(temperatureStr)) {
            try {
                temperatureSetting = new BigDecimal(temperatureStr);
            } catch (NumberFormatException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageDeviceLine",
                        "Invalid temperature setting '" + temperatureStr + "' on line " + lineNumber + " in " + fileName);
                return false;
            }
        }

        Integer capacityLimit = null;
        if (!GenericValidator.isBlankOrNull(capacityStr)) {
            try {
                capacityLimit = Integer.parseInt(capacityStr);
            } catch (NumberFormatException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageDeviceLine",
                        "Invalid capacity limit '" + capacityStr + "' on line " + lineNumber + " in " + fileName);
                return false;
            }
        }

        Boolean active = true;
        if (!GenericValidator.isBlankOrNull(activeStr)) {
            active = Boolean.parseBoolean(activeStr.toLowerCase());
        }

        StorageRoom parentRoom = storageRoomDAO.findByCode(parentRoomCode);
        if (parentRoom == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageDeviceLine",
                    "Skipping line " + lineNumber + " in " + fileName +
                    ": parent room with code '" + parentRoomCode + "' not found");
            return false;
        }

        StorageDevice existingDevice = storageDeviceDAO.findByCode(code);
        if (existingDevice != null) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "processStorageDeviceLine",
                    "Storage device with code '" + code + "' already exists. Skipping line " + lineNumber);
            return false;
        }

        try {
            StorageDevice device = new StorageDevice();
            device.setFhirUuid(UUID.randomUUID());
            device.setName(name);
            device.setCode(code);
            device.setTypeEnum(deviceType);
            device.setTemperatureSetting(temperatureSetting);
            device.setCapacityLimit(capacityLimit);
            device.setActive(active);
            device.setParentRoom(parentRoom);
            device.setSysUserId("1"); // System user

            storageDeviceDAO.insert(device);

            LogEvent.logInfo(this.getClass().getSimpleName(), "processStorageDeviceLine",
                    "Successfully created storage device '" + name + "' with code '" + code +
                    "' in room '" + parentRoom.getName() + "'");
            return true;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "processStorageDeviceLine",
                    "Failed to create storage device '" + name + "': " + e.getMessage());
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