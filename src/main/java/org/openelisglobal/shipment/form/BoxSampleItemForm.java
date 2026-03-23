package org.openelisglobal.shipment.form;

import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import org.openelisglobal.common.form.BaseForm;

/**
 * Form object for BoxSampleItem REST API requests and responses. Uses
 * SampleItem (not Sample) as the correct granularity for shipment operations.
 */
public class BoxSampleItemForm extends BaseForm {

    private Integer id;

    @NotNull(message = "Shipping box ID is required")
    private Integer shippingBoxId;

    @NotNull(message = "Sample item ID is required")
    private String sampleItemId;

    private String accessionNumber;

    private String typeOfSample;

    private Integer positionInBox;

    private String receptionStatus;

    private String receptionNotes;

    private Timestamp addedDate;

    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getShippingBoxId() {
        return shippingBoxId;
    }

    public void setShippingBoxId(Integer shippingBoxId) {
        this.shippingBoxId = shippingBoxId;
    }

    public String getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getTypeOfSample() {
        return typeOfSample;
    }

    public void setTypeOfSample(String typeOfSample) {
        this.typeOfSample = typeOfSample;
    }

    public Integer getPositionInBox() {
        return positionInBox;
    }

    public void setPositionInBox(Integer positionInBox) {
        this.positionInBox = positionInBox;
    }

    public String getReceptionStatus() {
        return receptionStatus;
    }

    public void setReceptionStatus(String receptionStatus) {
        this.receptionStatus = receptionStatus;
    }

    public String getReceptionNotes() {
        return receptionNotes;
    }

    public void setReceptionNotes(String receptionNotes) {
        this.receptionNotes = receptionNotes;
    }

    public Timestamp getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(Timestamp addedDate) {
        this.addedDate = addedDate;
    }
}
