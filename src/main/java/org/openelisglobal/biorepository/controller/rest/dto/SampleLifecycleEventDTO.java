package org.openelisglobal.biorepository.controller.rest.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class SampleLifecycleEventDTO {

    private Integer sampleItemId;
    private Integer bioSampleId;
    private String sampleExternalId;
    private String accessionNumber;
    private String eventType;
    private String custodyAction;
    private Timestamp eventTimestamp;
    private Timestamp actionTimestamp;
    private String stage;
    private String actorUserId;
    private String actorDisplayName;
    private String fromWorkflowStatus;
    private String toWorkflowStatus;
    private String fromLocationDisplay;
    private String fromLocation;
    private String toLocationDisplay;
    private String toLocation;
    private String storageCoordinates;
    private BigDecimal temperature;
    private String sourceRecordType;
    private Integer sourceRecordId;
    private String notes;

    public Integer getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(Integer sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public Integer getBioSampleId() {
        return bioSampleId;
    }

    public void setBioSampleId(Integer bioSampleId) {
        this.bioSampleId = bioSampleId;
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getCustodyAction() {
        return custodyAction;
    }

    public void setCustodyAction(String custodyAction) {
        this.custodyAction = custodyAction;
    }

    public Timestamp getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Timestamp eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public Timestamp getActionTimestamp() {
        return actionTimestamp;
    }

    public void setActionTimestamp(Timestamp actionTimestamp) {
        this.actionTimestamp = actionTimestamp;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(String actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorDisplayName() {
        return actorDisplayName;
    }

    public void setActorDisplayName(String actorDisplayName) {
        this.actorDisplayName = actorDisplayName;
    }

    public String getFromWorkflowStatus() {
        return fromWorkflowStatus;
    }

    public void setFromWorkflowStatus(String fromWorkflowStatus) {
        this.fromWorkflowStatus = fromWorkflowStatus;
    }

    public String getToWorkflowStatus() {
        return toWorkflowStatus;
    }

    public void setToWorkflowStatus(String toWorkflowStatus) {
        this.toWorkflowStatus = toWorkflowStatus;
    }

    public String getFromLocationDisplay() {
        return fromLocationDisplay;
    }

    public void setFromLocationDisplay(String fromLocationDisplay) {
        this.fromLocationDisplay = fromLocationDisplay;
        this.fromLocation = fromLocationDisplay;
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public String getToLocationDisplay() {
        return toLocationDisplay;
    }

    public void setToLocationDisplay(String toLocationDisplay) {
        this.toLocationDisplay = toLocationDisplay;
        this.toLocation = toLocationDisplay;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public String getStorageCoordinates() {
        return storageCoordinates;
    }

    public void setStorageCoordinates(String storageCoordinates) {
        this.storageCoordinates = storageCoordinates;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public String getSourceRecordType() {
        return sourceRecordType;
    }

    public void setSourceRecordType(String sourceRecordType) {
        this.sourceRecordType = sourceRecordType;
    }

    public Integer getSourceRecordId() {
        return sourceRecordId;
    }

    public void setSourceRecordId(Integer sourceRecordId) {
        this.sourceRecordId = sourceRecordId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
