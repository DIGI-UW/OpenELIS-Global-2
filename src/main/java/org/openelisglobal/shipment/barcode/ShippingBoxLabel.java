package org.openelisglobal.shipment.barcode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.openelisglobal.barcode.LabelField;
import org.openelisglobal.barcode.labeltype.Label;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.MessageUtil;

/**
 * Label for shipping boxes Displays box ID, destination, temperature, sample
 * count, and creation date
 */
public class ShippingBoxLabel extends Label {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    /**
     * Create label for a shipping box
     *
     * @param boxId                  The box identifier
     * @param destinationFacility    The destination facility name
     * @param temperatureRequirement The temperature requirement
     * @param sampleCount            The number of samples in the box
     * @param createdDate            The creation date
     */
    public ShippingBoxLabel(String boxId, String destinationFacility, String temperatureRequirement, int sampleCount,
            Date createdDate) {
        // Set dimensions from configuration properties or use defaults
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
                height = 1.5f; // Default height (slightly taller for more info)
            }
        } catch (Exception e) {
            LogEvent.logError("ShippingBoxLabel", "ShippingBoxLabel constructor", e.toString());
            // Use defaults if configuration fails
            width = 3.0f;
            height = 1.5f;
        }

        // Initialize fields
        aboveFields = new ArrayList<>();
        belowFields = new ArrayList<>();

        // Add box ID above barcode (prominent)
        LabelField boxIdField = new LabelField(MessageUtil.getMessage("barcode.label.info.boxid", "Box ID"),
                boxId != null ? boxId : "", 14);
        boxIdField.setDisplayFieldName(true);
        boxIdField.setUnderline(true);
        aboveFields.add(boxIdField);

        // Add destination facility above barcode
        LabelField destField = new LabelField(MessageUtil.getMessage("barcode.label.info.destination", "Destination"),
                destinationFacility != null ? destinationFacility : "", 10);
        destField.setDisplayFieldName(true);
        aboveFields.add(destField);

        // Add temperature requirement below barcode
        LabelField tempField = new LabelField(MessageUtil.getMessage("barcode.label.info.temperature", "Temperature"),
                temperatureRequirement != null ? temperatureRequirement : "AMBIENT", 9);
        tempField.setDisplayFieldName(true);
        belowFields.add(tempField);

        // Add sample count below barcode
        LabelField countField = new LabelField(MessageUtil.getMessage("barcode.label.info.samplecount", "Samples"),
                String.valueOf(sampleCount), 8);
        countField.setDisplayFieldName(true);
        belowFields.add(countField);

        // Add creation date below barcode
        String dateStr = createdDate != null ? DATE_FORMAT.format(createdDate) : "";
        LabelField dateField = new LabelField(MessageUtil.getMessage("barcode.label.info.created", "Created"), dateStr,
                8);
        dateField.setDisplayFieldName(true);
        belowFields.add(dateField);

        // Set barcode code to box ID
        setCode(boxId != null ? boxId : "");
        setCodeLabel(boxId != null ? boxId : "");
    }

    @Override
    public int getNumTextRowsBefore() {
        return getNumRows(aboveFields);
    }

    @Override
    public int getNumTextRowsAfter() {
        return getNumRows(belowFields);
    }

    @Override
    public int getMaxNumLabels() {
        // Shipping box labels can be printed multiple times (reprints allowed)
        return Integer.MAX_VALUE;
    }
}
