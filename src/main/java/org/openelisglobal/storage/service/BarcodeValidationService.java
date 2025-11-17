package org.openelisglobal.storage.service;

/**
 * Service for validating storage location barcodes Implements 5-step validation
 * process per FR-024 through FR-027: 1. Format validation 2. Location existence
 * check 3. Hierarchy validation 4. Activity check 5. Conflict check
 */
public interface BarcodeValidationService {

    /**
     * Validate a barcode through all 5 validation steps
     *
     * @param barcode The barcode string to validate
     * @return BarcodeValidationResponse with validation result and details
     */
    BarcodeValidationResponse validateBarcode(String barcode);
}
