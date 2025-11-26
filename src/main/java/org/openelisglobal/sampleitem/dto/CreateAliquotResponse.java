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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Response object for aliquot creation operations.
 *
 * <p>
 * Supports both single and bulk aliquot creation. When a single aliquot is
 * created, the 'aliquot' field contains the created aliquot. When multiple
 * aliquots are created, the 'aliquots' list contains all created aliquots and
 * 'aliquot' returns the first one for backward compatibility.
 *
 * <p>
 * Returns the created aliquot details, updated parent remaining quantity, and a
 * success message for user feedback.
 *
 * <p>
 * Related: Feature 001-sample-management, User Story 3
 *
 * @see org.openelisglobal.sampleitem.form.CreateAliquotForm
 */
public class CreateAliquotResponse {

    private List<SampleItemDTO> aliquots = new ArrayList<>();
    private BigDecimal parentUpdatedRemainingQuantity;
    private BigDecimal quantityPerAliquot;
    private String message;

    // ========== Constructors ==========

    public CreateAliquotResponse() {
    }

    /**
     * Constructor for single aliquot creation (backward compatible).
     */
    public CreateAliquotResponse(SampleItemDTO aliquot, BigDecimal parentUpdatedRemainingQuantity, String message) {
        this.aliquots = Collections.singletonList(aliquot);
        this.parentUpdatedRemainingQuantity = parentUpdatedRemainingQuantity;
        this.message = message;
    }

    /**
     * Constructor for bulk aliquot creation.
     */
    public CreateAliquotResponse(List<SampleItemDTO> aliquots, BigDecimal parentUpdatedRemainingQuantity,
            BigDecimal quantityPerAliquot, String message) {
        this.aliquots = aliquots != null ? aliquots : new ArrayList<>();
        this.parentUpdatedRemainingQuantity = parentUpdatedRemainingQuantity;
        this.quantityPerAliquot = quantityPerAliquot;
        this.message = message;
    }

    // ========== Getters and Setters ==========

    /**
     * Returns the first aliquot for backward compatibility. For bulk creation, use
     * getAliquots() to get all created aliquots.
     */
    public SampleItemDTO getAliquot() {
        return aliquots != null && !aliquots.isEmpty() ? aliquots.get(0) : null;
    }

    /**
     * Sets a single aliquot (for backward compatibility).
     */
    public void setAliquot(SampleItemDTO aliquot) {
        this.aliquots = Collections.singletonList(aliquot);
    }

    /**
     * Returns all created aliquots.
     */
    public List<SampleItemDTO> getAliquots() {
        return aliquots;
    }

    /**
     * Sets all created aliquots.
     */
    public void setAliquots(List<SampleItemDTO> aliquots) {
        this.aliquots = aliquots != null ? aliquots : new ArrayList<>();
    }

    /**
     * Returns the number of aliquots created.
     */
    public int getAliquotCount() {
        return aliquots != null ? aliquots.size() : 0;
    }

    public BigDecimal getParentUpdatedRemainingQuantity() {
        return parentUpdatedRemainingQuantity;
    }

    public void setParentUpdatedRemainingQuantity(BigDecimal parentUpdatedRemainingQuantity) {
        this.parentUpdatedRemainingQuantity = parentUpdatedRemainingQuantity;
    }

    public BigDecimal getQuantityPerAliquot() {
        return quantityPerAliquot;
    }

    public void setQuantityPerAliquot(BigDecimal quantityPerAliquot) {
        this.quantityPerAliquot = quantityPerAliquot;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
