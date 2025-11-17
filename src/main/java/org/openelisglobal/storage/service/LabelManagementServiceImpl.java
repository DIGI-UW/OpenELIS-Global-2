package org.openelisglobal.storage.service;

import java.io.ByteArrayOutputStream;
import javax.sql.DataSource;
import org.openelisglobal.barcode.BarcodeLabelMaker;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.storage.barcode.labeltype.StorageLocationLabel;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of LabelManagementService Generates PDF labels for storage
 * locations and tracks print history
 */
@Service
@Transactional
public class LabelManagementServiceImpl implements LabelManagementService {

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate getJdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayOutputStream generateLabel(StorageDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("Device cannot be null");
        }

        // Determine barcode code: use code if ≤10 chars, otherwise use short_code
        String barcodeCode = getBarcodeCode(device.getCode(), device.getShortCode());
        if (barcodeCode == null || barcodeCode.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Device code or short_code is required for label printing. If code > 10 chars, short_code must be set.");
        }

        // Build hierarchical path using codes (for barcode): RoomCode-DeviceCode
        StorageRoom parentRoom = device.getParentRoom();
        String hierarchicalPath = null;
        if (parentRoom != null && parentRoom.getCode() != null) {
            hierarchicalPath = parentRoom.getCode() + "-" + device.getCode();
        } else {
            hierarchicalPath = device.getCode();
        }

        // Create label using determined barcode code
        StorageLocationLabel label = new StorageLocationLabel(device.getName(), device.getCode(), hierarchicalPath,
                barcodeCode);

        // Generate PDF using BarcodeLabelMaker
        return generatePDF(label);
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayOutputStream generateLabel(StorageShelf shelf) {
        if (shelf == null) {
            throw new IllegalArgumentException("Shelf cannot be null");
        }

        // Determine barcode code: use label if ≤10 chars, otherwise use short_code
        String barcodeCode = getBarcodeCode(shelf.getLabel(), shelf.getShortCode());
        if (barcodeCode == null || barcodeCode.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Shelf label or short_code is required for label printing. If label > 10 chars, short_code must be set.");
        }

        // Build hierarchical path using codes: RoomCode-DeviceCode-ShelfLabel
        StorageDevice parentDevice = shelf.getParentDevice();
        String hierarchicalPath = null;
        if (parentDevice != null) {
            StorageRoom parentRoom = parentDevice.getParentRoom();
            if (parentRoom != null && parentRoom.getCode() != null) {
                hierarchicalPath = parentRoom.getCode() + "-" + parentDevice.getCode() + "-" + shelf.getLabel();
            } else {
                hierarchicalPath = parentDevice.getCode() + "-" + shelf.getLabel();
            }
        } else {
            hierarchicalPath = shelf.getLabel();
        }

        // Create label using determined barcode code
        StorageLocationLabel label = new StorageLocationLabel(shelf.getLabel(), shelf.getLabel(), hierarchicalPath,
                barcodeCode);

        // Generate PDF using BarcodeLabelMaker
        return generatePDF(label);
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayOutputStream generateLabel(StorageRack rack) {
        if (rack == null) {
            throw new IllegalArgumentException("Rack cannot be null");
        }

        // Determine barcode code: use label if ≤10 chars, otherwise use short_code
        String barcodeCode = getBarcodeCode(rack.getLabel(), rack.getShortCode());
        if (barcodeCode == null || barcodeCode.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Rack label or short_code is required for label printing. If label > 10 chars, short_code must be set.");
        }

        // Build hierarchical path using codes: RoomCode-DeviceCode-ShelfLabel-RackLabel
        StorageShelf parentShelf = rack.getParentShelf();
        String hierarchicalPath = null;
        if (parentShelf != null) {
            StorageDevice parentDevice = parentShelf.getParentDevice();
            if (parentDevice != null) {
                StorageRoom parentRoom = parentDevice.getParentRoom();
                if (parentRoom != null && parentRoom.getCode() != null) {
                    hierarchicalPath = parentRoom.getCode() + "-" + parentDevice.getCode() + "-"
                            + parentShelf.getLabel() + "-" + rack.getLabel();
                } else {
                    hierarchicalPath = parentDevice.getCode() + "-" + parentShelf.getLabel() + "-" + rack.getLabel();
                }
            } else {
                hierarchicalPath = parentShelf.getLabel() + "-" + rack.getLabel();
            }
        } else {
            hierarchicalPath = rack.getLabel();
        }

        // Create label using determined barcode code
        StorageLocationLabel label = new StorageLocationLabel(rack.getLabel(), rack.getLabel(), hierarchicalPath,
                barcodeCode);

        // Generate PDF using BarcodeLabelMaker
        return generatePDF(label);
    }

    /**
     * Determine barcode code: use primaryCode if ≤10 chars, otherwise use shortCode
     * 
     * @param primaryCode The primary code (device.code, shelf.label, rack.label)
     * @param shortCode   The short code (nullable)
     * @return The code to use for barcode generation
     */
    private String getBarcodeCode(String primaryCode, String shortCode) {
        if (primaryCode == null) {
            return shortCode;
        }
        // Use primaryCode if ≤10 chars, otherwise use shortCode
        if (primaryCode.length() <= 10) {
            return primaryCode;
        } else {
            // If primaryCode > 10 chars, shortCode is required
            return (shortCode != null && !shortCode.trim().isEmpty()) ? shortCode : null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateShortCodeExists(String locationId, String locationType) {
        if (locationId == null || locationType == null) {
            return false;
        }

        try {
            switch (locationType.toLowerCase()) {
            case "device":
                StorageDevice device = (StorageDevice) storageLocationService.get(Integer.parseInt(locationId),
                        StorageDevice.class);
                if (device == null) {
                    return false;
                }
                // Valid if code ≤10 chars OR shortCode exists
                // If code is null, check shortCode only
                if (device.getCode() == null || device.getCode().trim().isEmpty()) {
                    return device.getShortCode() != null && !device.getShortCode().trim().isEmpty();
                }
                // If code exists, valid if ≤10 chars OR shortCode exists
                return device.getCode().length() <= 10
                        || (device.getShortCode() != null && !device.getShortCode().trim().isEmpty());
            case "shelf":
                StorageShelf shelf = (StorageShelf) storageLocationService.get(Integer.parseInt(locationId),
                        StorageShelf.class);
                if (shelf == null) {
                    return false;
                }
                // Valid if label ≤10 chars OR shortCode exists
                return shelf.getLabel() != null && shelf.getLabel().length() <= 10
                        || (shelf.getShortCode() != null && !shelf.getShortCode().trim().isEmpty());
            case "rack":
                StorageRack rack = (StorageRack) storageLocationService.get(Integer.parseInt(locationId),
                        StorageRack.class);
                if (rack == null) {
                    return false;
                }
                // Valid if label ≤10 chars OR shortCode exists
                return rack.getLabel() != null && rack.getLabel().length() <= 10
                        || (rack.getShortCode() != null && !rack.getShortCode().trim().isEmpty());
            default:
                return false;
            }
        } catch (Exception e) {
            LogEvent.logError("LabelManagementServiceImpl", "validateShortCodeExists",
                    "Error validating short_code: " + e.getMessage());
            return false;
        }
    }

    /**
     * Generate PDF from label using BarcodeLabelMaker
     */
    private ByteArrayOutputStream generatePDF(StorageLocationLabel label) {
        try {
            // Link barcode label info (for print tracking)
            label.linkBarcodeLabelInfo();

            // Create BarcodeLabelMaker - use single-label constructor which properly
            // initializes
            // The no-arg constructor initializes barcodeType from configuration
            BarcodeLabelMaker labelMaker = new BarcodeLabelMaker(label);

            // Set number of labels to print (default 1)
            label.setNumLabels(1);

            // Generate PDF stream
            ByteArrayOutputStream stream = labelMaker.createLabelsAsStream();

            if (stream == null || stream.size() == 0) {
                LogEvent.logError("LabelManagementServiceImpl", "generatePDF", "PDF stream is null or empty!");
            }

            return stream;
        } catch (Exception e) {
            LogEvent.logError("LabelManagementServiceImpl", "generatePDF",
                    "Exception during PDF generation: " + e.getClass().getName() + " - " + e.getMessage());
            LogEvent.logError(e);
            if (e.getCause() != null) {
                LogEvent.logError("LabelManagementServiceImpl", "generatePDF",
                        "Caused by: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
            }
            throw new RuntimeException("Failed to generate label PDF", e);
        }
    }

    @Override
    @Transactional
    public void trackPrintHistory(String locationId, String locationType, String shortCode, String userId) {
        // Insert print history record into database
        getJdbcTemplate().update(
                "INSERT INTO storage_location_print_history (id, location_type, location_id, short_code, printed_by, printed_date, print_count) "
                        + "VALUES (gen_random_uuid(), ?, ?, ?, ?, CURRENT_TIMESTAMP, 1) "
                        + "ON CONFLICT (id) DO UPDATE SET print_count = storage_location_print_history.print_count + 1",
                locationType, locationId, shortCode, userId);
    }
}
