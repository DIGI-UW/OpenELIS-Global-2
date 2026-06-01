package org.openelisglobal.biorepository.service;

import java.math.BigDecimal;

/**
 * Per-sample metadata supplied when creating a biorepository transfer request.
 */
public class TransferItemMetadata {

    private Integer sampleItemId;
    private Integer sourceNotebookId;
    private Integer sourceNotebookEntryId;
    private String collectionDate;
    private BigDecimal quantity;
    private String unitOfMeasure;
    private String sampleCondition;
    private String preservationMedium;

    public TransferItemMetadata() {
    }

    public TransferItemMetadata(Integer sampleItemId, String sampleCondition, String preservationMedium) {
        this.sampleItemId = sampleItemId;
        this.sampleCondition = sampleCondition;
        this.preservationMedium = preservationMedium;
    }

    public Integer getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(Integer sampleItemId) {
        this.sampleItemId = sampleItemId;
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

    public String getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(String collectionDate) {
        this.collectionDate = collectionDate;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
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
}
