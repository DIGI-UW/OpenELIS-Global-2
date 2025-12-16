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

import java.sql.Timestamp;
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

    public SampleItem() {
        super();
        typeOfSample = new ValueHolder();
        sourceOfSample = new ValueHolder();
        unitOfMeasure = new ValueHolder();
        sample = new ValueHolder();
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
}
