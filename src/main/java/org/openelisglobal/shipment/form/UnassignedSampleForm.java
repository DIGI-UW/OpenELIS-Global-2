package org.openelisglobal.shipment.form;

import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import org.openelisglobal.common.form.BaseForm;

/**
 * Form object for UnassignedSample REST API requests and responses
 */
public class UnassignedSampleForm extends BaseForm {

    private Integer id;

    @NotNull(message = "Sample ID is required")
    private Integer sampleId;

    private String accessionNumber;

    @NotNull(message = "Referral test ID is required")
    private Integer referralTestId;

    private String referralTestName;

    private Integer destinationFacilityId;

    private String destinationFacilityName;

    private String priority;

    private Timestamp createdDate;

    private Timestamp assignedDate;

    private Boolean isAssigned;

    // Getters and Setters

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

    public Integer getReferralTestId() {
        return referralTestId;
    }

    public void setReferralTestId(Integer referralTestId) {
        this.referralTestId = referralTestId;
    }

    public String getReferralTestName() {
        return referralTestName;
    }

    public void setReferralTestName(String referralTestName) {
        this.referralTestName = referralTestName;
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

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public Timestamp getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(Timestamp assignedDate) {
        this.assignedDate = assignedDate;
    }

    public Boolean getIsAssigned() {
        return isAssigned;
    }

    public void setIsAssigned(Boolean isAssigned) {
        this.isAssigned = isAssigned;
    }
}
