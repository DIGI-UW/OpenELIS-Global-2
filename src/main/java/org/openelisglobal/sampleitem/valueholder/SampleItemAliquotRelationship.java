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
package org.openelisglobal.sampleitem.valueholder;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Entity representing the parent-child relationship between sample items during
 * aliquoting.
 *
 * <p>
 * This junction table tracks rich metadata about aliquoting operations beyond
 * the simple FK relationship in the sample_item table. It stores: - Sequence
 * numbering for aliquots (.1, .2, .3, etc.) - Quantity transferred to each
 * aliquot - Optional notes about the aliquoting operation - FHIR UUID for
 * external system integration
 *
 * <p>
 * The direct FK (parent_sample_item_id) in sample_item provides navigational
 * convenience, while this table provides audit trail and metadata for
 * compliance and troubleshooting.
 *
 * <p>
 * Constitution Compliance: Uses XML mapping
 * (SampleItemAliquotRelationship.hbm.xml) to maintain consistency with
 * SampleItem which also uses XML mapping.
 *
 * <p>
 * Related: Feature 001-sample-management
 *
 * @see SampleItem#parentSampleItem
 * @see SampleItem#childAliquots
 * @see <a href="../../../../specs/001-sample-management/data-model.md">Data
 *      Model Specification</a>
 */
public class SampleItemAliquotRelationship extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * Parent sample item from which the aliquot was created. Never null - every
     * aliquot relationship must have a parent.
     */
    private SampleItem parentSampleItem;

    /**
     * Child sample item (the aliquot) created from the parent. Never null - every
     * aliquot relationship must have a child.
     */
    private SampleItem childSampleItem;

    /**
     * Sequence number of this aliquot relative to its parent. Corresponds to the
     * suffix in external ID (e.g., .1, .2, .3). Unique constraint:
     * (parent_sample_item_id, sequence_number).
     */
    private Integer sequenceNumber;

    /**
     * Quantity transferred from parent to this aliquot. Must match the aliquot's
     * original_quantity. Precision 10,3 supports values like 123.456 mL.
     */
    private BigDecimal quantityTransferred;

    /**
     * Optional notes about the aliquoting operation. Examples: "For PCR testing",
     * "Sent to external lab", "Quality control sample". Max length: 1000
     * characters.
     */
    private String notes;

    /**
     * FHIR UUID for external system integration (FHIR R4 Specimen.parent). Maps to
     * FHIR Specimen resource representing this aliquot relationship. Unique across
     * all aliquot relationships.
     */
    private UUID fhirUuid;

    /**
     * Timestamp when this relationship was created. Automatically set on insert.
     */
    private Timestamp createdDate;

    // ========== Constructors ==========

    public SampleItemAliquotRelationship() {
        super();
    }

    // ========== Getters and Setters ==========

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public SampleItem getParentSampleItem() {
        return parentSampleItem;
    }

    public void setParentSampleItem(SampleItem parentSampleItem) {
        this.parentSampleItem = parentSampleItem;
    }

    public SampleItem getChildSampleItem() {
        return childSampleItem;
    }

    public void setChildSampleItem(SampleItem childSampleItem) {
        this.childSampleItem = childSampleItem;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public BigDecimal getQuantityTransferred() {
        return quantityTransferred;
    }

    public void setQuantityTransferred(BigDecimal quantityTransferred) {
        this.quantityTransferred = quantityTransferred;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    // ========== Utility Methods ==========

    @Override
    public String toString() {
        return "SampleItemAliquotRelationship{" + "id='" + id + '\'' + ", parentSampleItemId="
                + (parentSampleItem != null ? parentSampleItem.getId() : "null") + ", childSampleItemId="
                + (childSampleItem != null ? childSampleItem.getId() : "null") + ", sequenceNumber=" + sequenceNumber
                + ", quantityTransferred=" + quantityTransferred + ", fhirUuid=" + fhirUuid + '}';
    }
}
