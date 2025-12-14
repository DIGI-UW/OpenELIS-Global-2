package org.openelisglobal.inventory.valueholder.reports;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Report bean (DTO) for Transaction History reports Represents individual
 * inventory transactions with quantity changes Used with
 * JRBeanCollectionDataSource for JasperReports integration
 */
public class TransactionHistoryReportData {

    private String transactionId;
    private String transactionType;
    private String itemName;
    private String lotNumber;
    private String storageLocationPath;
    private String unitOfMeasure;
    private String performedBy;
    private String reason;
    private String referenceType;
    private String referenceId;
    private LocalDateTime transactionTimestamp;
    private BigDecimal quantityChange;
    private BigDecimal quantityAfterTransaction;

    // Default constructor
    public TransactionHistoryReportData() {
    }

    // Getters and Setters

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public String getStorageLocationPath() {
        return storageLocationPath;
    }

    public void setStorageLocationPath(String storageLocationPath) {
        this.storageLocationPath = storageLocationPath;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public LocalDateTime getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public void setTransactionTimestamp(LocalDateTime transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }

    public BigDecimal getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(BigDecimal quantityChange) {
        this.quantityChange = quantityChange;
    }

    public BigDecimal getQuantityAfterTransaction() {
        return quantityAfterTransaction;
    }

    public void setQuantityAfterTransaction(BigDecimal quantityAfterTransaction) {
        this.quantityAfterTransaction = quantityAfterTransaction;
    }
}
