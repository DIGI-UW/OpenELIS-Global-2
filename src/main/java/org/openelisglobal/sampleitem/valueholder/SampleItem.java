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

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.note.service.NoteObject;
import org.openelisglobal.note.service.NoteServiceImpl.BoundTo;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemServiceImpl;
import org.openelisglobal.sourceofsample.valueholder.SourceOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;

@Setter
@Getter
@DynamicUpdate
@Entity
@Table(name = "SAMPLE_ITEM")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class SampleItem extends BaseObject<String> implements NoteObject {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "sample_item_seq_gen")
    @GenericGenerator(name = "sample_item_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "sample_item_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;

    @Column(name = "QUANTITY", precision = 22, scale = 0)
    private Double quantity;

    @Column(name = "fhir_uuid")
    private UUID fhirUuid;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "SAMP_ID")
    private Sample sample;

    @Column(name = "SAMPITEM_ID", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String sampleItemId;

    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "SORT_ORDER", precision = 22, scale = 0)
    private String sortOrder;

    @Transient
    private SourceOfSample sourceOfSample;

    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "SOURCE_ID", precision = 10, scale = 0)
    private String sourceOfSampleId;

    @Column(name = "SOURCE_OTHER", length = 40)
    private String sourceOther;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "TYPEOSAMP_ID")
    private TypeOfSample typeOfSample;

    @Transient
    private String typeOfSampleId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "UOM_ID")
    private UnitOfMeasure unitOfMeasure;

    @Transient
    private String unitOfMeasureName;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "collection_date")
    private Timestamp collectionDate;

    @Column(name = "status_id")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String statusId;

    @Column(name = "collector")
    private String collector;

    @Column(name = "collection_conditions")
    private String collectionConditions;

    @Column(name = "collection_method")
    private String collectionMethod;

    @Column(name = "sample_temperature")
    private String sampleTemperature;

    @Column(name = "specimen_origin")
    private String specimenOrigin;

    @Column(name = "container")
    private String container;

    @Column(name = "location_details")
    private String locationDetails;

    @Column(name = "gps_latitude")
    private String gpsLatitude;

    @Column(name = "gps_longitude")
    private String gpsLongitude;

    @Column(name = "received_date")
    private Timestamp receivedDate;

    @Column(name = "rejected")
    private boolean rejected = false;

    @Column(name = "reject_reason_id")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String rejectReasonId;

    @Column(name = "voided")
    private boolean voided = false;

    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "lab_performed_sampling")
    private boolean labPerformedSampling = false;

    @Column(name = "collection_location_id")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String collectionLocationId;

    @Column(name = "collection_notes", length = 500)
    private String collectionNotes;

    // ========== Aliquoting Support Fields (Feature 001-sample-management)
    // ==========
    // These fields are mapped via SampleItem.hbm.xml

    /**
     * Remaining quantity available for aliquoting or testing. Decremented when
     * creating aliquots. Cannot be negative. If null, the quantity field should be
     * used as the remaining quantity (for legacy samples without aliquoting).
     */

    @Column(name = "remaining_quantity")
    private BigDecimal remainingQuantity;

    /**
     * Parent sample item if this is an aliquot. NULL for original samples,
     * references parent for aliquots. Enables recursive aliquoting (aliquots of
     * aliquots).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_sample_item_id")
    private SampleItem parentSampleItem;

    /**
     * Child aliquots created from this sample item. Empty for aliquots that haven't
     * been further divided. Enables querying sample hierarchy.
     */
    @OneToMany(mappedBy = "parentSampleItem", fetch = FetchType.LAZY)
    private List<SampleItem> childAliquots = new ArrayList<>();

    /**
     * Optimistic locking version for concurrency control during aliquoting.
     * Prevents race conditions when multiple users aliquot the same sample
     * concurrently. Mapped via hbm.xml as 'lastupdated' column.
     */
    @Transient
    private Timestamp version;

    public SampleItem() {
        super();
        childAliquots = new ArrayList<>();
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getTypeOfSampleId() {
        if (typeOfSampleId == null) {
            if (getTypeOfSample() != null) {
                typeOfSampleId = getTypeOfSample().getId();
            }
        }

        return typeOfSampleId;
    }

    public String getSourceOfSampleId() {
        if (sourceOfSampleId == null) {
            if (getSourceOfSample() != null) {
                sourceOfSampleId = getSourceOfSample().getId();
            }
        }
        return sourceOfSampleId;
    }

    @Override
    public String getTableId() {
        return SampleItemServiceImpl.getSampleItemTableReferenceId();
    }

    @Override
    public String getObjectId() {
        return getId();
    }

    @Override
    public BoundTo getBoundTo() {
        return BoundTo.SAMPLE_ITEM;
    }

    public String getFhirUuidAsString() {
        return fhirUuid == null ? "" : fhirUuid.toString();
    }

    /**
     * Get the effective remaining quantity, falling back to quantity if
     * remainingQuantity is null (for legacy samples).
     *
     * @return the remaining quantity, or quantity if remainingQuantity is null
     */
    public BigDecimal getEffectiveRemainingQuantity() {
        if (remainingQuantity != null) {
            return remainingQuantity;
        }
        // Fallback to quantity for legacy samples without remainingQuantity set
        return quantity != null ? BigDecimal.valueOf(quantity) : null;
    }

    /**
     * Add a child aliquot to this sample item. Helper method to maintain
     * bidirectional relationship.
     *
     * @param aliquot the child aliquot to add
     */
    public void addChildAliquot(SampleItem aliquot) {
        if (!childAliquots.contains(aliquot)) {
            childAliquots.add(aliquot);
            aliquot.setParentSampleItem(this);
        }
    }

    /**
     * Remove a child aliquot from this sample item. Helper method to maintain
     * bidirectional relationship.
     *
     * @param aliquot the child aliquot to remove
     */
    public void removeChildAliquot(SampleItem aliquot) {
        if (childAliquots.contains(aliquot)) {
            childAliquots.remove(aliquot);
            aliquot.setParentSampleItem(null);
        }
    }

    // ========== Aliquoting Helper Methods (Feature 001-sample-management, T009)
    // ==========

    /**
     * Check if this sample item has remaining quantity available for aliquoting.
     * Falls back to quantity field for legacy samples.
     *
     * @return true if effective remaining quantity is not null and greater than
     *         zero
     */
    public boolean hasRemainingQuantity() {
        BigDecimal effective = getEffectiveRemainingQuantity();
        return effective != null && effective.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if this sample item is an aliquot (has a parent).
     *
     * @return true if this sample has a parent sample item
     */
    public boolean isAliquot() {
        return parentSampleItem != null;
    }

    /**
     * Check if the requested quantity can be aliquoted from this sample. Validates
     * that sufficient remaining quantity exists. Falls back to quantity field for
     * legacy samples.
     *
     * @param requestedQuantity the quantity to aliquot
     * @return true if requested quantity is valid and does not exceed effective
     *         remaining quantity
     */
    public boolean canAliquot(BigDecimal requestedQuantity) {
        if (requestedQuantity == null || requestedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal effective = getEffectiveRemainingQuantity();
        if (effective == null) {
            return false;
        }
        return effective.compareTo(requestedQuantity) >= 0;
    }

    /**
     * Decrement the remaining quantity by the specified amount. Used when creating
     * aliquots to track volume dispensing.
     *
     * @param amount the quantity to subtract from remaining quantity
     * @throws IllegalArgumentException if amount is null, negative, or exceeds
     *                                  remaining quantity
     */
    public void decrementRemainingQuantity(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount to decrement cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount to decrement cannot be negative: " + amount);
        }
        // For legacy samples without remainingQuantity, initialize from original
        // quantity
        if (remainingQuantity == null) {
            if (quantity != null) {
                remainingQuantity = BigDecimal.valueOf(quantity);
            } else {
                throw new IllegalStateException(
                        "Cannot decrement remaining quantity: neither remainingQuantity nor quantity is set");
            }
        }
        if (remainingQuantity.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Cannot decrement " + amount + " from remaining quantity "
                    + remainingQuantity + ": insufficient quantity");
        }
        this.remainingQuantity = remainingQuantity.subtract(amount);
    }

    /**
     * Calculate the nesting level of this sample item in the aliquot hierarchy.
     * Original samples have level 0, their direct aliquots have level 1, etc.
     *
     * @return the nesting level (0 for original samples)
     */
    public int getNestingLevel() {
        int level = 0;
        SampleItem current = this.parentSampleItem;
        while (current != null) {
            level++;
            current = current.getParentSampleItem();
        }
        return level;
    }
}
