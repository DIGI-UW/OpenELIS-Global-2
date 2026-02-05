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
import org.openelisglobal.storage.dao.StorageBoxDAO;
import org.openelisglobal.storage.dao.StorageRackDAO;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration handler for creating storage boxes from CSV files.
 *
 * This handler processes CSV files to create storage box entries, which are
 * gridded containers (plates, sample boxes) within storage racks.
 *
 * CSV Format:
 * label,type,rows,columns,positionSchemaHint,code,active,parentRackCode
 * Example:
 * label,type,rows,columns,positionSchemaHint,code,active,parentRackCode Plate
 * ULF1-S1-R1-001,96-well,8,12,letter-number,P001,true,R1 Box
 * ULF1-S2-R1-001,9x9,9,9,number-number,B001,true,R1 Archive
 * 2024-001,10x10,10,10,number-number,A2024-001,true,AR1
 */
@Component
@Transactional
public class StorageBoxConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private StorageBoxDAO storageBoxDAO;

    @Autowired
    private StorageRackDAO storageRackDAO;

    @Override
    public String getDomainName() {
        return "storage-boxes";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 140; // After storage racks (130)
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (GenericValidator.isBlankOrNull(headerLine)) {
            throw new IllegalArgumentException("Storage box configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);

        int labelIndex = findColumnIndex(headers, "label");
        int typeIndex = findColumnIndex(headers, "type");
        int rowsIndex = findColumnIndex(headers, "rows");
        int columnsIndex = findColumnIndex(headers, "columns");
        int positionSchemaIndex = findColumnIndex(headers, "positionSchemaHint");
        int codeIndex = findColumnIndex(headers, "code");
        int activeIndex = findColumnIndex(headers, "active");
        int parentRackCodeIndex = findColumnIndex(headers, "parentRackCode");

        if (labelIndex == -1) {
            throw new IllegalArgumentException(
                    "Storage box configuration file " + fileName + " must have a 'label' column");
        }
        if (typeIndex == -1) {
            throw new IllegalArgumentException(
                    "Storage box configuration file " + fileName + " must have a 'type' column");
        }
        if (rowsIndex == -1) {
            throw new IllegalArgumentException(
                    "Storage box configuration file " + fileName + " must have a 'rows' column");
        }
        if (columnsIndex == -1) {
            throw new IllegalArgumentException(
                    "Storage box configuration file " + fileName + " must have a 'columns' column");
        }
        if (codeIndex == -1) {
            throw new IllegalArgumentException(
                    "Storage box configuration file " + fileName + " must have a 'code' column");
        }
        if (parentRackCodeIndex == -1) {
            throw new IllegalArgumentException(
                    "Storage box configuration file " + fileName + " must have a 'parentRackCode' column");
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
                boolean processed = processStorageBoxLine(line, labelIndex, typeIndex, rowsIndex, columnsIndex,
                        positionSchemaIndex, codeIndex, activeIndex, parentRackCodeIndex, fileName, lineNumber);
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
                "Storage box configuration processing completed for " + fileName + ". Processed: " + processedCount
                        + ", Skipped: " + skippedCount);
    }

    private boolean processStorageBoxLine(String line, int labelIndex, int typeIndex, int rowsIndex, int columnsIndex,
            int positionSchemaIndex, int codeIndex, int activeIndex, int parentRackCodeIndex, String fileName,
            int lineNumber) {

        String[] values = parseCsvLine(line);

        String label = getValueOrEmpty(values, labelIndex);
        String type = getValueOrEmpty(values, typeIndex);
        String rowsStr = getValueOrEmpty(values, rowsIndex);
        String columnsStr = getValueOrEmpty(values, columnsIndex);
        String positionSchemaHint = getValueOrEmpty(values, positionSchemaIndex);
        String code = getValueOrEmpty(values, codeIndex);
        String activeStr = getValueOrEmpty(values, activeIndex);
        String parentRackCode = getValueOrEmpty(values, parentRackCodeIndex);

        // Validate required fields
        if (GenericValidator.isBlankOrNull(label)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageBoxLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing box label");
            return false;
        }

        if (GenericValidator.isBlankOrNull(type)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageBoxLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing box type");
            return false;
        }

        if (GenericValidator.isBlankOrNull(code)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageBoxLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing box code");
            return false;
        }

        if (GenericValidator.isBlankOrNull(parentRackCode)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageBoxLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing parent rack code");
            return false;
        }

        // Parse rows (required)
        Integer rows;
        if (GenericValidator.isBlankOrNull(rowsStr)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageBoxLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing rows");
            return false;
        }
        try {
            rows = Integer.parseInt(rowsStr);
        } catch (NumberFormatException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageBoxLine",
                    "Invalid rows value '" + rowsStr + "' on line " + lineNumber + " in " + fileName);
            return false;
        }

        // Parse columns (required)
        Integer columns;
        if (GenericValidator.isBlankOrNull(columnsStr)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageBoxLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing columns");
            return false;
        }
        try {
            columns = Integer.parseInt(columnsStr);
        } catch (NumberFormatException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageBoxLine",
                    "Invalid columns value '" + columnsStr + "' on line " + lineNumber + " in " + fileName);
            return false;
        }

        // Parse active flag (default to true)
        Boolean active = true;
        if (!GenericValidator.isBlankOrNull(activeStr)) {
            active = Boolean.parseBoolean(activeStr.toLowerCase());
        }

        // Find parent rack
        StorageRack parentRack = storageRackDAO.findByCode(parentRackCode);
        if (parentRack == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processStorageBoxLine", "Skipping line " + lineNumber
                    + " in " + fileName + ": parent rack with code '" + parentRackCode + "' not found");
            return false;
        }

        // Check if box with this code already exists
        List<StorageBox> existingBoxes = storageBoxDAO.getAllMatching("code", code);
        if (!existingBoxes.isEmpty()) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "processStorageBoxLine",
                    "Storage box with code '" + code + "' already exists. Skipping line " + lineNumber);
            return false;
        }

        // Create new storage box
        try {
            StorageBox box = new StorageBox();
            box.setFhirUuid(UUID.randomUUID());
            box.setLabel(label);
            box.setType(type);
            box.setRows(rows);
            box.setColumns(columns);
            box.setPositionSchemaHint(positionSchemaHint);
            box.setCode(code);
            box.setActive(active);
            box.setParentRack(parentRack);
            box.setSysUserId("1"); // System user

            storageBoxDAO.insert(box);

            LogEvent.logInfo(this.getClass().getSimpleName(), "processStorageBoxLine",
                    "Successfully created storage box '" + label + "' with code '" + code + "' in rack '"
                            + parentRack.getLabel() + "'");
            return true;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "processStorageBoxLine",
                    "Failed to create storage box '" + label + "': " + e.getMessage());
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
