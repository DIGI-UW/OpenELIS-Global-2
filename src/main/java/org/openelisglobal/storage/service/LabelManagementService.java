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
     * Generate PDF label for a storage device
     * 
     * @param device    The storage device
     * @param shortCode Optional short code (if provided, used for barcode;
     *                  otherwise uses hierarchical path)
     * @return PDF as ByteArrayOutputStream
     */
    ByteArrayOutputStream generateLabel(StorageDevice device, String shortCode);

    /**
     * Generate PDF label for a storage shelf
     * 
     * @param shelf     The storage shelf
     * @param shortCode Optional short code (if provided, used for barcode;
     *                  otherwise uses hierarchical path)
     * @return PDF as ByteArrayOutputStream
     */
    ByteArrayOutputStream generateLabel(StorageShelf shelf, String shortCode);

    /**
     * Generate PDF label for a storage rack
     * 
     * @param rack      The storage rack
     * @param shortCode Optional short code (if provided, used for barcode;
     *                  otherwise uses hierarchical path)
     * @return PDF as ByteArrayOutputStream
     */
    ByteArrayOutputStream generateLabel(StorageRack rack, String shortCode);

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
