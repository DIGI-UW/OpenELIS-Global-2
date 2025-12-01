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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.note.service.NoteObject;
import org.openelisglobal.note.service.NoteServiceImpl.BoundTo;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemServiceImpl;
import org.openelisglobal.sourceofsample.valueholder.SourceOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;

public class SampleItem extends BaseObject<String> implements NoteObject {

    private static final long serialVersionUID = 1L;

    private String id;

    private Double quantity;

    private UUID fhirUuid;
    private ValueHolderInterface sample;
    private String sampleItemId;
    private String sortOrder;
    private ValueHolderInterface sourceOfSample;
    private String sourceOfSampleId;
    private String sourceOther;
    private ValueHolderInterface typeOfSample;
    private String typeOfSampleId;
    private ValueHolderInterface unitOfMeasure;
    private String unitOfMeasureName;
    private String externalId;
    private Timestamp collectionDate;
    private String statusId;
    private String collector;
    private boolean rejected = false;
    private String rejectReasonId;
    private boolean voided = false;
    private String voidReason;

    // ========== Aliquoting Support Fields (Feature 001-sample-management)
    // ==========
    // These fields are mapped via SampleItem.hbm.xml

    /**
     * Remaining quantity available for aliquoting or testing. Decremented when
     * creating aliquots. Cannot be negative. If null, the quantity field should be
     * used as the remaining quantity (for legacy samples without aliquoting).
     */
    private BigDecimal remainingQuantity;

    /**
     * Parent sample item if this is an aliquot. NULL for original samples,
     * references parent for aliquots. Enables recursive aliquoting (aliquots of
     * aliquots).
     */
    private SampleItem parentSampleItem;

    /**
     * Child aliquots created from this sample item. Empty for aliquots that haven't
     * been further divided. Enables querying sample hierarchy.
     */
    private List<SampleItem> childAliquots = new ArrayList<>();

    /**
     * Optimistic locking version for concurrency control during aliquoting.
     * Prevents race conditions when multiple users aliquot the same sample
     * concurrently. Mapped via hbm.xml as 'lastupdated' column.
     */
    private Timestamp version;

    public SampleItem() {
        super();
        typeOfSample = new ValueHolder();
        sourceOfSample = new ValueHolder();
        unitOfMeasure = new ValueHolder();
        sample = new ValueHolder();
        childAliquots = new ArrayList<>();
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Timestamp getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(Timestamp collectionDate) {
        this.collectionDate = collectionDate;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getQuantity() {
        return quantity;
    }

    public String getTypeOfSampleId() {
        if (typeOfSampleId == null) {
            if (getTypeOfSample() != null) {
                typeOfSampleId = getTypeOfSample().getId();
            }
        }

        return typeOfSampleId;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getSourceOfSampleId() {
        if (sourceOfSampleId == null) {
            if (getSourceOfSample() != null) {
                sourceOfSampleId = getSourceOfSample().getId();
            }
        }
        return sourceOfSampleId;
    }

    public void setSourceOfSampleId(String sourceOfSampleId) {
        this.sourceOfSampleId = sourceOfSampleId;
    }

    public String getSourceOther() {
        return sourceOther;
    }

    public void setSourceOther(String sourceOther) {
        this.sourceOther = sourceOther;
    }

    public Sample getSample() {
        return (Sample) sample.getValue();
    }

    public void setSample(Sample sample) {
        this.sample.setValue(sample);
    }

    public String getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public TypeOfSample getTypeOfSample() {
        return (TypeOfSample) typeOfSample.getValue();
    }

    public void setTypeOfSample(TypeOfSample typeOfSample) {
        this.typeOfSample.setValue(typeOfSample);
    }

    public SourceOfSample getSourceOfSample() {
        return (SourceOfSample) sourceOfSample.getValue();
    }

    public void setSourceOfSample(SourceOfSample sourceOfSample) {
        this.sourceOfSample.setValue(sourceOfSample);
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return (UnitOfMeasure) unitOfMeasure.getValue();
    }

    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasure.setValue(unitOfMeasure);
    }

    public String getUnitOfMeasureName() {
        return unitOfMeasureName;
    }

    public void setUnitOfMeasureName(String unitOfMeasureName) {
        this.unitOfMeasureName = unitOfMeasureName;
    }

    public String getStatusId() {
        return statusId;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    public String getCollector() {
        return collector;
    }

    public void setCollector(String collector) {
        this.collector = collector;
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

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public String getFhirUuidAsString() {
        return fhirUuid == null ? "" : fhirUuid.toString();
    }

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }

    public String getRejectReasonId() {
        return rejectReasonId;
    }

    public void setRejectReasonId(String rejectReasonId) {
        this.rejectReasonId = rejectReasonId;
    }

    public boolean isVoided() {
        return voided;
    }

    public void setVoided(boolean voided) {
        this.voided = voided;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public void setVoidReason(String voidReason) {
        this.voidReason = voidReason;
    }

    // ========== Aliquoting Getters/Setters (Feature 001-sample-management)
    // ==========

    public BigDecimal getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(BigDecimal remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
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

    public SampleItem getParentSampleItem() {
        return parentSampleItem;
    }

    public void setParentSampleItem(SampleItem parentSampleItem) {
        this.parentSampleItem = parentSampleItem;
    }

    public List<SampleItem> getChildAliquots() {
        return childAliquots;
    }

    public void setChildAliquots(List<SampleItem> childAliquots) {
        this.childAliquots = childAliquots;
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
