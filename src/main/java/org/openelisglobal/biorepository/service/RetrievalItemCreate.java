package org.openelisglobal.biorepository.service;

import java.math.BigDecimal;

/**
 * Per-sample quantity metadata supplied when creating a biorepository retrieval request.
 */
public class RetrievalItemCreate {

    private Integer bioSampleId;
    private BigDecimal quantityRequested;
    private String unitOfMeasure;

    public RetrievalItemCreate() {
    }

    public RetrievalItemCreate(Integer bioSampleId, BigDecimal quantityRequested, String unitOfMeasure) {
        this.bioSampleId = bioSampleId;
        this.quantityRequested = quantityRequested;
        this.unitOfMeasure = unitOfMeasure;
    }

    public Integer getBioSampleId() {
        return bioSampleId;
    }

    public void setBioSampleId(Integer bioSampleId) {
        this.bioSampleId = bioSampleId;
    }

    public BigDecimal getQuantityRequested() {
        return quantityRequested;
    }

    public void setQuantityRequested(BigDecimal quantityRequested) {
        this.quantityRequested = quantityRequested;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }
}
