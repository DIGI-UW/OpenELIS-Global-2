package org.openelisglobal.storage.service;

import java.io.ByteArrayOutputStream;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageShelf;

/**
 * Service for generating barcode labels for storage locations Handles label
 * generation, print history tracking
 */
public interface LabelManagementService {

    /**
     * Generate PDF label for a storage device Uses short_code from the device
     * entity
     * 
     * @param device The storage device (must have short_code set)
     * @return PDF as ByteArrayOutputStream
     * @throws IllegalArgumentException if device is null or short_code is missing
     */
    ByteArrayOutputStream generateLabel(StorageDevice device);

    /**
     * Generate PDF label for a storage shelf Uses short_code from the shelf entity
     * 
     * @param shelf The storage shelf (must have short_code set)
     * @return PDF as ByteArrayOutputStream
     * @throws IllegalArgumentException if shelf is null or short_code is missing
     */
    ByteArrayOutputStream generateLabel(StorageShelf shelf);

    /**
     * Generate PDF label for a storage rack Uses short_code from the rack entity
     * 
     * @param rack The storage rack (must have short_code set)
     * @return PDF as ByteArrayOutputStream
     * @throws IllegalArgumentException if rack is null or short_code is missing
     */
    ByteArrayOutputStream generateLabel(StorageRack rack);

    /**
     * Validate that short_code exists for a location before printing
     * 
     * @param locationId   The ID of the location
     * @param locationType The type: "device", "shelf", or "rack"
     * @return true if short_code exists, false otherwise
     */
    boolean validateShortCodeExists(String locationId, String locationType);

    /**
     * Track print history for a location Records audit trail of label printing
     * 
     * @param locationId   The ID of the location
     * @param locationType The type: "device", "shelf", or "rack"
     * @param shortCode    The short code used (if any)
     * @param userId       The user ID who printed the label
     */
    void trackPrintHistory(String locationId, String locationType, String shortCode, String userId);
}
