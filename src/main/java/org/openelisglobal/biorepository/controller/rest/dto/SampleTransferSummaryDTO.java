package org.openelisglobal.biorepository.controller.rest.dto;

import java.sql.Timestamp;

public class SampleTransferSummaryDTO {

    private Integer transferRequestId;
    private Integer transferItemId;
    private String sourceLab;
    private String destinationLab;
    private String transferStatus;
    private String itemStatus;
    private String projectName;
    private String transferReason;
    private String requestNotes;
    private String requestedByName;
    private Timestamp requestedTimestamp;
    private String processedByName;
    private Timestamp processedTimestamp;
    private String rejectionReason;
    private String sampleExternalId;
    private String accessionNumber;
    private String sampleType;
    private Double quantity;
    private String unitOfMeasure;
    private String sampleCondition;
    private String preservationMedium;
    private Timestamp collectionDate;
    private Integer sourceNotebookId;
    private Integer sourceNotebookEntryId;
    private Integer sourceStorageAssignmentId;
    private Integer sourceStorageLocationId;
    private String sourceStorageLocationType;
    private String sourceStoragePositionCoordinate;
    private String sourceStorageLocation;
    private String biosafetyLevel;
    private String ethicsApprovalRef;
    private String originLab;
    private String principalInvestigator;
    private String projectId;

    public Integer getTransferRequestId() {
        return transferRequestId;
    }

    public void setTransferRequestId(Integer transferRequestId) {
        this.transferRequestId = transferRequestId;
    }

    public Integer getTransferItemId() {
        return transferItemId;
    }

    public void setTransferItemId(Integer transferItemId) {
        this.transferItemId = transferItemId;
    }

    public String getSourceLab() {
        return sourceLab;
    }

    public void setSourceLab(String sourceLab) {
        this.sourceLab = sourceLab;
    }

    public String getDestinationLab() {
        return destinationLab;
    }

    public void setDestinationLab(String destinationLab) {
        this.destinationLab = destinationLab;
    }

    public String getTransferStatus() {
        return transferStatus;
    }

    public void setTransferStatus(String transferStatus) {
        this.transferStatus = transferStatus;
    }

    public String getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(String itemStatus) {
        this.itemStatus = itemStatus;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getTransferReason() {
        return transferReason;
    }

    public void setTransferReason(String transferReason) {
        this.transferReason = transferReason;
    }

    public String getRequestNotes() {
        return requestNotes;
    }

    public void setRequestNotes(String requestNotes) {
        this.requestNotes = requestNotes;
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

    public String getProcessedByName() {
        return processedByName;
    }

    public void setProcessedByName(String processedByName) {
        this.processedByName = processedByName;
    }

    public Timestamp getProcessedTimestamp() {
        return processedTimestamp;
    }

    public void setProcessedTimestamp(Timestamp processedTimestamp) {
        this.processedTimestamp = processedTimestamp;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getSampleExternalId() {
        return sampleExternalId;
    }

    public void setSampleExternalId(String sampleExternalId) {
        this.sampleExternalId = sampleExternalId;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getSampleCondition() {
        return sampleCondition;
    }

    public void setSampleCondition(String sampleCondition) {
        this.sampleCondition = sampleCondition;
    }

    public String getPreservationMedium() {
        return preservationMedium;
    }

    public void setPreservationMedium(String preservationMedium) {
        this.preservationMedium = preservationMedium;
    }

    public Timestamp getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(Timestamp collectionDate) {
        this.collectionDate = collectionDate;
    }

    public Integer getSourceNotebookId() {
        return sourceNotebookId;
    }

    public void setSourceNotebookId(Integer sourceNotebookId) {
        this.sourceNotebookId = sourceNotebookId;
    }

    public Integer getSourceNotebookEntryId() {
        return sourceNotebookEntryId;
    }

    public void setSourceNotebookEntryId(Integer sourceNotebookEntryId) {
        this.sourceNotebookEntryId = sourceNotebookEntryId;
    }

    public Integer getSourceStorageAssignmentId() {
        return sourceStorageAssignmentId;
    }

    public void setSourceStorageAssignmentId(Integer sourceStorageAssignmentId) {
        this.sourceStorageAssignmentId = sourceStorageAssignmentId;
    }

    public Integer getSourceStorageLocationId() {
        return sourceStorageLocationId;
    }

    public void setSourceStorageLocationId(Integer sourceStorageLocationId) {
        this.sourceStorageLocationId = sourceStorageLocationId;
    }

    public String getSourceStorageLocationType() {
        return sourceStorageLocationType;
    }

    public void setSourceStorageLocationType(String sourceStorageLocationType) {
        this.sourceStorageLocationType = sourceStorageLocationType;
    }

    public String getSourceStoragePositionCoordinate() {
        return sourceStoragePositionCoordinate;
    }

    public void setSourceStoragePositionCoordinate(String sourceStoragePositionCoordinate) {
        this.sourceStoragePositionCoordinate = sourceStoragePositionCoordinate;
    }

    public String getSourceStorageLocation() {
        return sourceStorageLocation;
    }

    public void setSourceStorageLocation(String sourceStorageLocation) {
        this.sourceStorageLocation = sourceStorageLocation;
    }

    public String getBiosafetyLevel() {
        return biosafetyLevel;
    }

    public void setBiosafetyLevel(String biosafetyLevel) {
        this.biosafetyLevel = biosafetyLevel;
    }

    public String getEthicsApprovalRef() {
        return ethicsApprovalRef;
    }

    public void setEthicsApprovalRef(String ethicsApprovalRef) {
        this.ethicsApprovalRef = ethicsApprovalRef;
    }

    public String getOriginLab() {
        return originLab;
    }

    public void setOriginLab(String originLab) {
        this.originLab = originLab;
    }

    public String getPrincipalInvestigator() {
        return principalInvestigator;
    }

    public void setPrincipalInvestigator(String principalInvestigator) {
        this.principalInvestigator = principalInvestigator;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
