package org.openelisglobal.shipment.dto;

/**
 * A specimen the sending lab declared in a shipped box (from the
 * SupplyDelivery's EXT_SPECIMEN references), resolved against the referral
 * electronic order it belongs to on the receiving lab.
 *
 * <ul>
 * <li>PENDING — a matching electronic order exists but no Sample has been
 * accepted yet (offer the operator the pre-filled sample-entry form).</li>
 * <li>LINKED — a Sample exists for the order and is now linked to the box (a
 * BoxSampleItem).</li>
 * <li>UNRESOLVED — no matching electronic order found locally (the referral has
 * not been imported yet, or identifiers do not match).</li>
 * </ul>
 */
public class ExpectedSpecimenDTO {

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
