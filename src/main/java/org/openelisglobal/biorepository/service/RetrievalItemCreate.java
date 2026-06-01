package org.openelisglobal.biorepository.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Per-sample metadata supplied when creating a biorepository retrieval request.
 * bioSampleId is optional for reference-only request lines from external departments.
 */
public class RetrievalItemCreate {

    private Integer bioSampleId;
    private BigDecimal quantityRequested;
    private String unitOfMeasure;
    private String remark;
    private String requestedAccessionNumber;
    private String requestedBarcode;
    private String requestedSampleType;
    private String requestedOriginLab;
    private String requestedProjectId;
    private LocalDate requestedCollectionDateFrom;
    private LocalDate requestedCollectionDateTo;

    public RetrievalItemCreate() {
    }

    public RetrievalItemCreate(Integer bioSampleId, BigDecimal quantityRequested, String unitOfMeasure) {
        this.bioSampleId = bioSampleId;
        this.quantityRequested = quantityRequested;
        this.unitOfMeasure = unitOfMeasure;
    }

    public boolean isReferenceOnly() {
        return bioSampleId == null;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getRequestedAccessionNumber() {
        return requestedAccessionNumber;
    }

    public void setRequestedAccessionNumber(String requestedAccessionNumber) {
        this.requestedAccessionNumber = requestedAccessionNumber;
    }

    public String getRequestedBarcode() {
        return requestedBarcode;
    }

    public void setRequestedBarcode(String requestedBarcode) {
        this.requestedBarcode = requestedBarcode;
    }

    public String getRequestedSampleType() {
        return requestedSampleType;
    }

    public void setRequestedSampleType(String requestedSampleType) {
        this.requestedSampleType = requestedSampleType;
    }

    public String getRequestedOriginLab() {
        return requestedOriginLab;
    }

    public void setRequestedOriginLab(String requestedOriginLab) {
        this.requestedOriginLab = requestedOriginLab;
    }

    public String getRequestedProjectId() {
        return requestedProjectId;
    }

    public void setRequestedProjectId(String requestedProjectId) {
        this.requestedProjectId = requestedProjectId;
    }

    public LocalDate getRequestedCollectionDateFrom() {
        return requestedCollectionDateFrom;
    }

    public void setRequestedCollectionDateFrom(LocalDate requestedCollectionDateFrom) {
        this.requestedCollectionDateFrom = requestedCollectionDateFrom;
    }

    public LocalDate getRequestedCollectionDateTo() {
        return requestedCollectionDateTo;
    }

    public void setRequestedCollectionDateTo(LocalDate requestedCollectionDateTo) {
        this.requestedCollectionDateTo = requestedCollectionDateTo;
    }
}
