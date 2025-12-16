/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.sampleitem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for SampleItem entity.
 *
 * <p>
 * Used to transfer sample item data between service layer and REST controllers,
 * including aliquoting-related fields and relationships. This DTO compiles all
 * relevant data within transaction boundaries to prevent
 * LazyInitializationException per Constitution III.7.
 *
 * <p>
 * Related: Feature 001-sample-management
 *
 * @see org.openelisglobal.sampleitem.valueholder.SampleItem
 */
public class SampleItemDTO {

    private String id;
    private String externalId;
    private String sampleAccessionNumber;
    private String sampleType;
    private String sampleTypeId;
    private Double quantity; // Original quantity from legacy column
    private BigDecimal remainingQuantity; // Remaining quantity for aliquoting (null means use quantity)
    private String unitOfMeasure;
    private String unitOfMeasureId;
    private String status;
    private String statusId;
    private Timestamp collectionDate;

    // Parent-child relationship fields
    private String parentId;
    private String parentExternalId;
    private List<AliquotSummaryDTO> childAliquots = new ArrayList<>();

    // Associated tests
    private List<TestSummaryDTO> orderedTests = new ArrayList<>();

    // Computed fields
    private boolean hasRemainingQuantity;
    private boolean isAliquot;
    private int nestingLevel;

    // Metadata
    private Timestamp lastupdated;

    // ========== Constructors ==========

    public SampleItemDTO() {
    }

    // ========== Getters and Setters ==========

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getSampleAccessionNumber() {
        return sampleAccessionNumber;
    }

    public void setSampleAccessionNumber(String sampleAccessionNumber) {
        this.sampleAccessionNumber = sampleAccessionNumber;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getSampleTypeId() {
        return sampleTypeId;
    }

    public void setSampleTypeId(String sampleTypeId) {
        this.sampleTypeId = sampleTypeId;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(BigDecimal remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    /**
     * Get effective remaining quantity, falling back to quantity if
     * remainingQuantity is null. This matches the behavior in SampleItem entity.
     *
     * @return effective remaining quantity
     */
    @JsonProperty("effectiveRemainingQuantity")
    public BigDecimal getEffectiveRemainingQuantity() {
        if (remainingQuantity != null) {
            return remainingQuantity;
        }
        return quantity != null ? BigDecimal.valueOf(quantity) : null;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getUnitOfMeasureId() {
        return unitOfMeasureId;
    }

    public void setUnitOfMeasureId(String unitOfMeasureId) {
        this.unitOfMeasureId = unitOfMeasureId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusId() {
        return statusId;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    public Timestamp getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(Timestamp collectionDate) {
        this.collectionDate = collectionDate;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getParentExternalId() {
        return parentExternalId;
    }

    public void setParentExternalId(String parentExternalId) {
        this.parentExternalId = parentExternalId;
    }

    public List<AliquotSummaryDTO> getChildAliquots() {
        return childAliquots;
    }

    public void setChildAliquots(List<AliquotSummaryDTO> childAliquots) {
        this.childAliquots = childAliquots;
    }

    public List<TestSummaryDTO> getOrderedTests() {
        return orderedTests;
    }

    public void setOrderedTests(List<TestSummaryDTO> orderedTests) {
        this.orderedTests = orderedTests;
    }

    public boolean isHasRemainingQuantity() {
        return hasRemainingQuantity;
    }

    public void setHasRemainingQuantity(boolean hasRemainingQuantity) {
        this.hasRemainingQuantity = hasRemainingQuantity;
    }

    public boolean isAliquot() {
        return isAliquot;
    }

    public void setAliquot(boolean aliquot) {
        isAliquot = aliquot;
    }

    public int getNestingLevel() {
        return nestingLevel;
    }

    public void setNestingLevel(int nestingLevel) {
        this.nestingLevel = nestingLevel;
    }

    public Timestamp getLastupdated() {
        return lastupdated;
    }

    public void setLastupdated(Timestamp lastupdated) {
        this.lastupdated = lastupdated;
    }
}
