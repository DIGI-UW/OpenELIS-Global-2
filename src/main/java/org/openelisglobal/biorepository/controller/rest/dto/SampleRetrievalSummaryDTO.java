package org.openelisglobal.biorepository.controller.rest.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

public class SampleRetrievalSummaryDTO {

    private Integer retrievalRequestId;
    private Integer retrievalItemId;
    private String requestNumber;
    private String workOrderNumber;
    private String requestPurpose;
    private String requestStatus;
    private String itemStatus;
    private String requestorName;
    private String requesterLabUnit;
    private String principalInvestigator;
    private String projectTitle;
    private String requesterContactInfo;
    private String destinationDetails;
    private String requestedByName;
    private Timestamp requestedTimestamp;
    private String approvedByName;
    private Timestamp approvedTimestamp;
    private BigDecimal quantityRequested;
    private BigDecimal quantityReleased;
    private String unitOfMeasure;
    private String remark;
    private String conditionAtRelease;
    private String checkoutStoragePath;
    private String checkoutStorageCoordinates;
    private LocalDate expectedReturnDate;
    private Integer notebookEntryId;
    private Timestamp releasedTimestamp;
    private String receivedByName;

    public Integer getRetrievalRequestId() {
        return retrievalRequestId;
    }

    public void setRetrievalRequestId(Integer retrievalRequestId) {
        this.retrievalRequestId = retrievalRequestId;
    }

    public Integer getRetrievalItemId() {
        return retrievalItemId;
    }

    public void setRetrievalItemId(Integer retrievalItemId) {
        this.retrievalItemId = retrievalItemId;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }

    public String getRequestPurpose() {
        return requestPurpose;
    }

    public void setRequestPurpose(String requestPurpose) {
        this.requestPurpose = requestPurpose;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(String itemStatus) {
        this.itemStatus = itemStatus;
    }

    public String getRequestorName() {
        return requestorName;
    }

    public void setRequestorName(String requestorName) {
        this.requestorName = requestorName;
    }

    public String getRequesterLabUnit() {
        return requesterLabUnit;
    }

    public void setRequesterLabUnit(String requesterLabUnit) {
        this.requesterLabUnit = requesterLabUnit;
    }

    public String getPrincipalInvestigator() {
        return principalInvestigator;
    }

    public void setPrincipalInvestigator(String principalInvestigator) {
        this.principalInvestigator = principalInvestigator;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public String getRequesterContactInfo() {
        return requesterContactInfo;
    }

    public void setRequesterContactInfo(String requesterContactInfo) {
        this.requesterContactInfo = requesterContactInfo;
    }

    public String getDestinationDetails() {
        return destinationDetails;
    }

    public void setDestinationDetails(String destinationDetails) {
        this.destinationDetails = destinationDetails;
    }

    public String getRequestedByName() {
        return requestedByName;
    }

    public void setRequestedByName(String requestedByName) {
        this.requestedByName = requestedByName;
    }

    public Timestamp getRequestedTimestamp() {
        return requestedTimestamp;
    }

    public void setRequestedTimestamp(Timestamp requestedTimestamp) {
        this.requestedTimestamp = requestedTimestamp;
    }

    public String getApprovedByName() {
        return approvedByName;
    }

    public void setApprovedByName(String approvedByName) {
        this.approvedByName = approvedByName;
    }

    public Timestamp getApprovedTimestamp() {
        return approvedTimestamp;
    }

    public void setApprovedTimestamp(Timestamp approvedTimestamp) {
        this.approvedTimestamp = approvedTimestamp;
    }

    public BigDecimal getQuantityRequested() {
        return quantityRequested;
    }

    public void setQuantityRequested(BigDecimal quantityRequested) {
        this.quantityRequested = quantityRequested;
    }

    public BigDecimal getQuantityReleased() {
        return quantityReleased;
    }

    public void setQuantityReleased(BigDecimal quantityReleased) {
        this.quantityReleased = quantityReleased;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getConditionAtRelease() {
        return conditionAtRelease;
    }

    public void setConditionAtRelease(String conditionAtRelease) {
        this.conditionAtRelease = conditionAtRelease;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getCheckoutStoragePath() {
        return checkoutStoragePath;
    }

    public void setCheckoutStoragePath(String checkoutStoragePath) {
        this.checkoutStoragePath = checkoutStoragePath;
    }

    public String getCheckoutStorageCoordinates() {
        return checkoutStorageCoordinates;
    }

    public void setCheckoutStorageCoordinates(String checkoutStorageCoordinates) {
        this.checkoutStorageCoordinates = checkoutStorageCoordinates;
    }

    public LocalDate getExpectedReturnDate() {
        return expectedReturnDate;
    }

    public void setExpectedReturnDate(LocalDate expectedReturnDate) {
        this.expectedReturnDate = expectedReturnDate;
    }

    public Integer getNotebookEntryId() {
        return notebookEntryId;
    }

    public void setNotebookEntryId(Integer notebookEntryId) {
        this.notebookEntryId = notebookEntryId;
    }

    public Timestamp getReleasedTimestamp() {
        return releasedTimestamp;
    }

    public void setReleasedTimestamp(Timestamp releasedTimestamp) {
        this.releasedTimestamp = releasedTimestamp;
    }

    public String getReceivedByName() {
        return receivedByName;
    }

    public void setReceivedByName(String receivedByName) {
        this.receivedByName = receivedByName;
    }
}
