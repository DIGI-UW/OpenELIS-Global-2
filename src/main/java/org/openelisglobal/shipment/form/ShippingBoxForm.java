package org.openelisglobal.shipment.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;

/**
 * Form object for ShippingBox REST API requests and responses
 */
public class ShippingBoxForm extends BaseForm {

    private Integer id;

    @NotBlank(message = "Box ID is required")
    private String boxId;

    @NotNull(message = "Destination facility is required")
    private Integer destinationFacilityId;

    private String destinationFacilityName;

    @NotBlank(message = "Box state is required")
    private String state;

    private String temperatureRequirement;

    private Integer capacity;

    private Integer actualSampleCount;

    private String notes;

    private Timestamp createdDate;

    private Integer createdBy;

    private String createdByName;

    private Timestamp sentDate;

    private Timestamp receivedDate;

    private Timestamp reconciledDate;

    private Boolean archived = false;

    private Timestamp archivedDate;

    private Integer sampleCount;

    private List<BoxSampleInfo> samples;

    // Nested class for sample information
    public static class BoxSampleInfo {
        private Integer id;
        private Integer sampleId;
        private String accessionNumber;
        private String receptionStatus;
        private String receptionNotes;
        private Timestamp addedDate;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer getSampleId() {
            return sampleId;
        }

        public void setSampleId(Integer sampleId) {
            this.sampleId = sampleId;
        }

        public String getAccessionNumber() {
            return accessionNumber;
        }

        public void setAccessionNumber(String accessionNumber) {
            this.accessionNumber = accessionNumber;
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

    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBoxId() {
        return boxId;
    }

    public void setBoxId(String boxId) {
        this.boxId = boxId;
    }

    public Integer getDestinationFacilityId() {
        return destinationFacilityId;
    }

    public void setDestinationFacilityId(Integer destinationFacilityId) {
        this.destinationFacilityId = destinationFacilityId;
    }

    public String getDestinationFacilityName() {
        return destinationFacilityName;
    }

    public void setDestinationFacilityName(String destinationFacilityName) {
        this.destinationFacilityName = destinationFacilityName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTemperatureRequirement() {
        return temperatureRequirement;
    }

    public void setTemperatureRequirement(String temperatureRequirement) {
        this.temperatureRequirement = temperatureRequirement;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getActualSampleCount() {
        return actualSampleCount;
    }

    public void setActualSampleCount(Integer actualSampleCount) {
        this.actualSampleCount = actualSampleCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public Timestamp getSentDate() {
        return sentDate;
    }

    public void setSentDate(Timestamp sentDate) {
        this.sentDate = sentDate;
    }

    public Timestamp getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Timestamp receivedDate) {
        this.receivedDate = receivedDate;
    }

    public Timestamp getReconciledDate() {
        return reconciledDate;
    }

    public void setReconciledDate(Timestamp reconciledDate) {
        this.reconciledDate = reconciledDate;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Timestamp getArchivedDate() {
        return archivedDate;
    }

    public void setArchivedDate(Timestamp archivedDate) {
        this.archivedDate = archivedDate;
    }

    public Integer getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(Integer sampleCount) {
        this.sampleCount = sampleCount;
    }

    public List<BoxSampleInfo> getSamples() {
        return samples;
    }

    public void setSamples(List<BoxSampleInfo> samples) {
        this.samples = samples;
    }
}
