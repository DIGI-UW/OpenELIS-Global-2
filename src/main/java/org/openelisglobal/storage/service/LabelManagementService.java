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
     * Generate PDF label for a storage device Uses code from the device entity
     * 
     * @param device The storage device (must have code set and ≤10 chars)
     * @return PDF as ByteArrayOutputStream
     * @throws IllegalArgumentException if device is null or code is missing
     */
    ByteArrayOutputStream generateLabel(StorageDevice device);

    /**
     * Generate PDF label for a storage shelf Uses code from the shelf entity
     * 
     * @param shelf The storage shelf (must have code set and ≤10 chars)
     * @return PDF as ByteArrayOutputStream
     * @throws IllegalArgumentException if shelf is null or code is missing
     */
    ByteArrayOutputStream generateLabel(StorageShelf shelf);

    /**
     * Generate PDF label for a storage rack Uses code from the rack entity
     * 
     * @param rack The storage rack (must have code set and ≤10 chars)
     * @return PDF as ByteArrayOutputStream
     * @throws IllegalArgumentException if rack is null or code is missing
     */
    ByteArrayOutputStream generateLabel(StorageRack rack);

    /**
     * Validate that code exists for a location before printing
     * 
     * @param locationId   The ID of the location
     * @param locationType The type: "device", "shelf", or "rack"
     * @return true if code exists, false otherwise
     */
    boolean validateCodeExists(String locationId, String locationType);

    /**
     * Track print history for a location Records audit trail of label printing
     * 
     * @param locationId   The ID of the location
     * @param locationType The type: "device", "shelf", or "rack"
     * @param code         The code used for the label
     * @param userId       The user ID who printed the label
     */
    void trackPrintHistory(String locationId, String locationType, String code, String userId);
}
