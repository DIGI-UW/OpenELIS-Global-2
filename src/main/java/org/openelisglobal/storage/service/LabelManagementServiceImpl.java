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
    public ByteArrayOutputStream generateLabel(StorageDevice device, String shortCode) {
        if (device == null) {
            throw new IllegalArgumentException("Device cannot be null");
        }

        // Build hierarchical path using codes (for barcode): RoomCode-DeviceCode
        StorageRoom parentRoom = device.getParentRoom();
        String hierarchicalPath = null;
        if (parentRoom != null && parentRoom.getCode() != null) {
            hierarchicalPath = parentRoom.getCode() + "-" + device.getCode();
        } else {
            hierarchicalPath = device.getCode();
        }

        // Create label
        StorageLocationLabel label = new StorageLocationLabel(device.getName(), device.getCode(), hierarchicalPath,
                shortCode);

        // Generate PDF using BarcodeLabelMaker
        return generatePDF(label);
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayOutputStream generateLabel(StorageShelf shelf, String shortCode) {
        if (shelf == null) {
            throw new IllegalArgumentException("Shelf cannot be null");
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

        // Create label
        StorageLocationLabel label = new StorageLocationLabel(shelf.getLabel(), shelf.getLabel(), hierarchicalPath,
                shortCode);

        // Generate PDF using BarcodeLabelMaker
        return generatePDF(label);
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayOutputStream generateLabel(StorageRack rack, String shortCode) {
        if (rack == null) {
            throw new IllegalArgumentException("Rack cannot be null");
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

        // Create label
        StorageLocationLabel label = new StorageLocationLabel(rack.getLabel(), rack.getLabel(), hierarchicalPath,
                shortCode);

        // Generate PDF using BarcodeLabelMaker
        return generatePDF(label);
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
