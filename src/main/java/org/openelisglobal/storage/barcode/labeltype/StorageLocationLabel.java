package org.openelisglobal.storage.barcode.labeltype;

import java.util.ArrayList;
import org.openelisglobal.barcode.LabelField;
import org.openelisglobal.barcode.labeltype.Label;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.MessageUtil;

/**
 * Label for storage locations (Device, Shelf, Rack) Displays location name,
 * code, hierarchical path, and barcode Uses short code or hierarchical path for
 * barcode value
 */
public class StorageLocationLabel extends Label {

    /**
     * Create label for a storage location
     * 
     * @param locationName     The display name of the location
     * @param locationCode     The code of the location
     * @param hierarchicalPath The full hierarchical path (e.g.,
     *                         "MAIN-FRZ01-SHA-RKR1")
     * @param shortCode        Optional short code (if provided, used for barcode;
     *                         otherwise uses hierarchical path)
     */
    public StorageLocationLabel(String locationName, String locationCode, String hierarchicalPath, String shortCode) {
        // Set dimensions from configuration properties
        try {
            String widthStr = ConfigurationProperties.getInstance()
                    .getPropertyValue(Property.STORAGE_LOCATION_BARCODE_WIDTH);
            String heightStr = ConfigurationProperties.getInstance()
                    .getPropertyValue(Property.STORAGE_LOCATION_BARCODE_HEIGHT);

            if (widthStr != null && !widthStr.isEmpty()) {
                width = Float.parseFloat(widthStr);
            } else {
                width = 3.0f; // Default width
            }

            if (heightStr != null && !heightStr.isEmpty()) {
                height = Float.parseFloat(heightStr);
            } else {
                height = 1.0f; // Default height
            }
        } catch (Exception e) {
            LogEvent.logError("StorageLocationLabel", "StorageLocationLabel constructor", e.toString());
            // Use defaults if configuration fails
            width = 3.0f;
            height = 1.0f;
        }

        // Initialize fields
        aboveFields = new ArrayList<>();
        belowFields = new ArrayList<>();

        // Add location name above barcode
        LabelField nameField = new LabelField(
                MessageUtil.getMessage("barcode.label.info.locationname", "Location Name"),
                locationName != null ? locationName : "", 12);
        nameField.setDisplayFieldName(true);
        nameField.setUnderline(true);
        aboveFields.add(nameField);

        // Add location code above barcode
        LabelField codeField = new LabelField(MessageUtil.getMessage("barcode.label.info.locationcode", "Code"),
                locationCode != null ? locationCode : "", 8);
        codeField.setDisplayFieldName(true);
        aboveFields.add(codeField);

        // Add hierarchical path below barcode
        LabelField pathField = new LabelField(MessageUtil.getMessage("barcode.label.info.hierarchicalpath", "Path"),
                hierarchicalPath != null ? hierarchicalPath : "", 8);
        pathField.setDisplayFieldName(true);
        belowFields.add(pathField);

        // Set barcode code: use short code if provided, otherwise use hierarchical path
        String barcodeValue = (shortCode != null && !shortCode.trim().isEmpty()) ? shortCode.toUpperCase().trim()
                : (hierarchicalPath != null ? hierarchicalPath : locationCode);
        setCode(barcodeValue);
        setCodeLabel(barcodeValue);
    }

    @Override
    public int getNumTextRowsBefore() {
        // Count rows for above fields
        int numRows = 0;
        int curColumns = 0;
        boolean completeRow = true;

        for (LabelField field : aboveFields) {
            if (field.isStartNewline() && !completeRow) {
                ++numRows;
                curColumns = 0;
            }
            curColumns += field.getColspan();
            if (curColumns > 20) {
                // Row overflow - should not happen with proper colspan
            } else if (curColumns == 20) {
                completeRow = true;
                curColumns = 0;
                ++numRows;
            } else {
                completeRow = false;
            }
        }

        if (!completeRow) {
            ++numRows;
        }

        return numRows;
    }

    @Override
    public int getNumTextRowsAfter() {
        // Count rows for below fields
        int numRows = 0;
        int curColumns = 0;
        boolean completeRow = true;

        for (LabelField field : belowFields) {
            if (field.isStartNewline() && !completeRow) {
                ++numRows;
                curColumns = 0;
            }
            curColumns += field.getColspan();
            if (curColumns > 20) {
                // Row overflow
            } else if (curColumns == 20) {
                completeRow = true;
                curColumns = 0;
                ++numRows;
            } else {
                completeRow = false;
            }
        }

        if (!completeRow) {
            ++numRows;
        }

        return numRows;
    }

    @Override
    public int getMaxNumLabels() {
        // Storage location labels have no maximum print limit
        // Return a very large number to effectively disable the limit
        return Integer.MAX_VALUE;
    }
}
