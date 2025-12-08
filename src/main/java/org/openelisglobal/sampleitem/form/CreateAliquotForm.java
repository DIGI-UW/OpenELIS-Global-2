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
package org.openelisglobal.sampleitem.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Form object for creating aliquots from a parent sample item.
 *
 * <p>
 * Supports creating multiple equal-volume aliquots by specifying
 * numberOfAliquots. When numberOfAliquots is 1 (default), creates a single
 * aliquot with the specified quantity. When numberOfAliquots is greater than 1,
 * divides the total quantity equally among all aliquots.
 *
 * <p>
 * Includes Jakarta validation annotations for request validation at the
 * controller layer per Constitution CR-008 (input validation).
 *
 * <p>
 * Related: Feature 001-sample-management, User Story 3
 *
 * @see org.openelisglobal.sampleitem.dto.CreateAliquotResponse
 */
public class CreateAliquotForm {

    @NotBlank(message = "Parent sample item ID is required")
    private String parentSampleItemId;

    @NotNull(message = "Quantity to transfer is required")
    @DecimalMin(value = "0.001", inclusive = true, message = "Quantity must be at least 0.001")
    private BigDecimal quantityToTransfer;

    @Min(value = 1, message = "Number of aliquots must be at least 1")
    @Max(value = 100, message = "Number of aliquots cannot exceed 100")
    private int numberOfAliquots = 1;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    // ========== Constructors ==========

    public CreateAliquotForm() {
    }

    public CreateAliquotForm(String parentSampleItemId, BigDecimal quantityToTransfer, String notes) {
        this.parentSampleItemId = parentSampleItemId;
        this.quantityToTransfer = quantityToTransfer;
        this.notes = notes;
    }

    public CreateAliquotForm(String parentSampleItemId, BigDecimal quantityToTransfer, int numberOfAliquots,
            String notes) {
        this.parentSampleItemId = parentSampleItemId;
        this.quantityToTransfer = quantityToTransfer;
        this.numberOfAliquots = numberOfAliquots;
        this.notes = notes;
    }

    // ========== Getters and Setters ==========

    public String getParentSampleItemId() {
        return parentSampleItemId;
    }

    public void setParentSampleItemId(String parentSampleItemId) {
        this.parentSampleItemId = parentSampleItemId;
    }

    public BigDecimal getQuantityToTransfer() {
        return quantityToTransfer;
    }

    public void setQuantityToTransfer(BigDecimal quantityToTransfer) {
        this.quantityToTransfer = quantityToTransfer;
    }

    public int getNumberOfAliquots() {
        return numberOfAliquots;
    }

    public void setNumberOfAliquots(int numberOfAliquots) {
        this.numberOfAliquots = numberOfAliquots;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
