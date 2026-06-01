package org.openelisglobal.biorepository.controller.rest.dto;

/**
 * Optional identity override for bulk fulfillment suggestion lookup.
 */
public class RetrievalItemIdentityLookupDTO {

    private Integer retrievalItemId;
    private String accessionNumber;
    private String barcode;

    public Integer getRetrievalItemId() {
        return retrievalItemId;
    }

    public void setRetrievalItemId(Integer retrievalItemId) {
        this.retrievalItemId = retrievalItemId;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
}
