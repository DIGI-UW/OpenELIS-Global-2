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

/**
 * Summary Data Transfer Object for aliquot sample items.
 *
 * <p>
 * Lightweight DTO used for displaying aliquot summaries in parent sample item
 * details. Contains essential information without full nested relationships to
 * prevent excessive data loading.
 *
 * <p>
 * Related: Feature 001-sample-management
 *
 * @see SampleItemDTO#childAliquots
 */
public class AliquotSummaryDTO {

    private String id;
    private String externalId;
    private Double quantity; // Original quantity from legacy column
    private BigDecimal remainingQuantity; // Remaining quantity (null means use quantity)
    private Timestamp createdDate;

    // ========== Constructors ==========

    public AliquotSummaryDTO() {
    }

    public AliquotSummaryDTO(String id, String externalId, Double quantity, BigDecimal remainingQuantity,
            Timestamp createdDate) {
        this.id = id;
        this.externalId = externalId;
        this.quantity = quantity;
        this.remainingQuantity = remainingQuantity;
        this.createdDate = createdDate;
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
     * remainingQuantity is null.
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

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }
}
