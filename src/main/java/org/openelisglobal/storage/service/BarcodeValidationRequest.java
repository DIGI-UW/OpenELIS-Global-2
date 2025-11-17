package org.openelisglobal.storage.service;

/**
 * Request object for barcode validation
 */
public class BarcodeValidationRequest {

    private String barcode;

    public BarcodeValidationRequest() {
    }

    public BarcodeValidationRequest(String barcode) {
        this.barcode = barcode;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
}
