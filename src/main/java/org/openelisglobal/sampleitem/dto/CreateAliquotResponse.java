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

/**
 * Response object for aliquot creation operations.
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

    private SampleItemDTO aliquot;
    private BigDecimal parentUpdatedRemainingQuantity;
    private String message;

    // ========== Constructors ==========

    public CreateAliquotResponse() {
    }

    public CreateAliquotResponse(SampleItemDTO aliquot, BigDecimal parentUpdatedRemainingQuantity, String message) {
        this.aliquot = aliquot;
        this.parentUpdatedRemainingQuantity = parentUpdatedRemainingQuantity;
        this.message = message;
    }

    // ========== Getters and Setters ==========

    public SampleItemDTO getAliquot() {
        return aliquot;
    }

    public void setAliquot(SampleItemDTO aliquot) {
        this.aliquot = aliquot;
    }

    public BigDecimal getParentUpdatedRemainingQuantity() {
        return parentUpdatedRemainingQuantity;
    }

    public void setParentUpdatedRemainingQuantity(BigDecimal parentUpdatedRemainingQuantity) {
        this.parentUpdatedRemainingQuantity = parentUpdatedRemainingQuantity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
