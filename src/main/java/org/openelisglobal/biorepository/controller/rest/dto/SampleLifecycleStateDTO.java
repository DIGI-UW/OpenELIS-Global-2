package org.openelisglobal.biorepository.controller.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class SampleLifecycleStateDTO {

    private String workflowStatus;
    private boolean physicallyInStorage;
    private String lastKnownStorageLocation;
    private String currentCustodian;
    private Integer activeTransferRequestId;
    private Integer activeRetrievalRequestId;
    private boolean awaitingRestorage;
    private BigDecimal quantityRequested;
    private BigDecimal quantityReleased;
    private BigDecimal remainingQuantity;
    private String unitOfMeasure;

    public String getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(String workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    @JsonProperty("isPhysicallyInStorage")
    public boolean isPhysicallyInStorage() {
        return physicallyInStorage;
    }

    public void setPhysicallyInStorage(boolean physicallyInStorage) {
        this.physicallyInStorage = physicallyInStorage;
    }

    public String getLastKnownStorageLocation() {
        return lastKnownStorageLocation;
    }

    public void setLastKnownStorageLocation(String lastKnownStorageLocation) {
        this.lastKnownStorageLocation = lastKnownStorageLocation;
    }

    public String getCurrentCustodian() {
        return currentCustodian;
    }

    public void setCurrentCustodian(String currentCustodian) {
        this.currentCustodian = currentCustodian;
    }

    public Integer getActiveTransferRequestId() {
        return activeTransferRequestId;
    }

    public void setActiveTransferRequestId(Integer activeTransferRequestId) {
        this.activeTransferRequestId = activeTransferRequestId;
    }

    public Integer getActiveRetrievalRequestId() {
        return activeRetrievalRequestId;
    }

    public void setActiveRetrievalRequestId(Integer activeRetrievalRequestId) {
        this.activeRetrievalRequestId = activeRetrievalRequestId;
    }

    public boolean isAwaitingRestorage() {
        return awaitingRestorage;
    }

    public void setAwaitingRestorage(boolean awaitingRestorage) {
        this.awaitingRestorage = awaitingRestorage;
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

    public BigDecimal getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(BigDecimal remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }
}
