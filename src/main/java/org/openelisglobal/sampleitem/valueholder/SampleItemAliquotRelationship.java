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

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
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
@Setter
@Getter
@DynamicUpdate
@Entity
@Table(name = "sample_item_aliquot_relationship")
public class SampleItemAliquotRelationship extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /**
     * Parent sample item from which the aliquot was created. Never null - every
     * aliquot relationship must have a parent.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_sample_item_id", nullable = false)
    private SampleItem parentSampleItem;

    /**
     * Child sample item (the aliquot) created from the parent. Never null - every
     * aliquot relationship must have a child.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_sample_item_id", nullable = false)
    private SampleItem childSampleItem;

    /**
     * Sequence number of this aliquot relative to its parent. Corresponds to the
     * suffix in external ID (e.g., .1, .2, .3). Unique constraint:
     * (parent_sample_item_id, sequence_number).
     */
    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    /**
     * Quantity transferred from parent to this aliquot. Must match the aliquot's
     * original_quantity. Precision 10,3 supports values like 123.456 mL.
     */
    @Column(name = "quantity_transferred", precision = 10, scale = 3, nullable = false)
    private BigDecimal quantityTransferred;

    /**
     * Optional notes about the aliquoting operation. Examples: "For PCR testing",
     * "Sent to external lab", "Quality control sample". Max length: 1000
     * characters.
     */
    @Column(name = "notes", length = 1000)
    private String notes;

    /**
     * FHIR UUID for external system integration (FHIR R4 Specimen.parent). Maps to
     * FHIR Specimen resource representing this aliquot relationship. Unique across
     * all aliquot relationships.
     */
    @Column(name = "fhir_uuid")
    private UUID fhirUuid;

    /**
     * Timestamp when this relationship was created. Automatically set on insert.
     */
    @CreationTimestamp
    @Column(name = "lastupdated", updatable = false)
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

    // ========== Utility Methods ==========

    @Override
    public String toString() {
        return "SampleItemAliquotRelationship{" + "id='" + id + '\'' + ", parentSampleItemId="
                + (parentSampleItem != null ? parentSampleItem.getId() : "null") + ", childSampleItemId="
                + (childSampleItem != null ? childSampleItem.getId() : "null") + ", sequenceNumber=" + sequenceNumber
                + ", quantityTransferred=" + quantityTransferred + ", fhirUuid=" + fhirUuid + '}';
    }

    @Transient
    @Override
    public Timestamp getLastupdated() {
        return super.getLastupdated();
    }

    @Override
    public void setLastupdated(Timestamp lastupdated) {
        super.setLastupdated(lastupdated);
    }
}
