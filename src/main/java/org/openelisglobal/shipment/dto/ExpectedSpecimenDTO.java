package org.openelisglobal.shipment.dto;

/**
 * A box specimen resolved against its referral electronic order on the
 * receiving lab.
 */
public class ExpectedSpecimenDTO {

    // PENDING: order exists, not yet accepted. LINKED: Sample accepted and linked
    // to the box.
    // UNRESOLVED: no matching electronic order found locally.
    public enum Status {
        PENDING, LINKED, UNRESOLVED
    }

    private String specimenUuid;
    private String typeDisplay;
    private String externalOrderNumber;
    private Status status;
    private Integer boxSampleItemId;

    public String getSpecimenUuid() {
        return specimenUuid;
    }

    public void setSpecimenUuid(String specimenUuid) {
        this.specimenUuid = specimenUuid;
    }

    public String getTypeDisplay() {
        return typeDisplay;
    }

    public void setTypeDisplay(String typeDisplay) {
        this.typeDisplay = typeDisplay;
    }

    public String getExternalOrderNumber() {
        return externalOrderNumber;
    }

    public void setExternalOrderNumber(String externalOrderNumber) {
        this.externalOrderNumber = externalOrderNumber;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Integer getBoxSampleItemId() {
        return boxSampleItemId;
    }

    public void setBoxSampleItemId(Integer boxSampleItemId) {
        this.boxSampleItemId = boxSampleItemId;
    }
}
