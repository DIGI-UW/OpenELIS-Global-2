package org.openelisglobal.biorepository.controller.rest.dto;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * DTO for listing biorepository samples with enriched data. Combines data from
 * BioSample, SampleItem, and Sample entities.
 */
public class BioSampleListDTO {

    private Integer id;
    private String barcode;
    private SampleTypeDTO sampleType;
    private String originLab;
    private Timestamp receiptDate;
    private Timestamp collectionDate;
    private String status;
    private String documentationStatus;
    private String biosafetyLevel;
    private String projectId;
    private String principalInvestigator;
    private String ethicsApprovalRef;
    private String mtaReference;
    private String preservationMedium;
    private String arrivalCondition;
    private Integer shipmentId;
    private Integer sampleItemId;
    private Integer sampleId;
    private String accessionNumber;
    private BigDecimal requiredTempMin;
    private BigDecimal requiredTempMax;

    // Retention policy fields
    private Integer retentionPolicyId;
    private String retentionPolicyName;
    private Date retentionExpiryDate;

    // Workflow status - tracks sample progression through biorepository lifecycle
    private String workflowStatus;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public SampleTypeDTO getSampleType() {
        return sampleType;
    }

    public void setSampleType(SampleTypeDTO sampleType) {
        this.sampleType = sampleType;
    }

    public String getOriginLab() {
        return originLab;
    }

    public void setOriginLab(String originLab) {
        this.originLab = originLab;
    }

    public Timestamp getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(Timestamp receiptDate) {
        this.receiptDate = receiptDate;
    }

    public Timestamp getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(Timestamp collectionDate) {
        this.collectionDate = collectionDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDocumentationStatus() {
        return documentationStatus;
    }

    public void setDocumentationStatus(String documentationStatus) {
        this.documentationStatus = documentationStatus;
    }

    public String getBiosafetyLevel() {
        return biosafetyLevel;
    }

    public void setBiosafetyLevel(String biosafetyLevel) {
        this.biosafetyLevel = biosafetyLevel;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getPrincipalInvestigator() {
        return principalInvestigator;
    }

    public void setPrincipalInvestigator(String principalInvestigator) {
        this.principalInvestigator = principalInvestigator;
    }

    public String getEthicsApprovalRef() {
        return ethicsApprovalRef;
    }

    public void setEthicsApprovalRef(String ethicsApprovalRef) {
        this.ethicsApprovalRef = ethicsApprovalRef;
    }

    public String getMtaReference() {
        return mtaReference;
    }

    public void setMtaReference(String mtaReference) {
        this.mtaReference = mtaReference;
    }

    public String getPreservationMedium() {
        return preservationMedium;
    }

    public void setPreservationMedium(String preservationMedium) {
        this.preservationMedium = preservationMedium;
    }

    public String getArrivalCondition() {
        return arrivalCondition;
    }

    public void setArrivalCondition(String arrivalCondition) {
        this.arrivalCondition = arrivalCondition;
    }

    public Integer getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Integer shipmentId) {
        this.shipmentId = shipmentId;
    }

    public Integer getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(Integer sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public Integer getSampleId() {
        return sampleId;
    }

    public void setSampleId(Integer sampleId) {
        this.sampleId = sampleId;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public BigDecimal getRequiredTempMin() {
        return requiredTempMin;
    }

    public void setRequiredTempMin(BigDecimal requiredTempMin) {
        this.requiredTempMin = requiredTempMin;
    }

    public BigDecimal getRequiredTempMax() {
        return requiredTempMax;
    }

    public void setRequiredTempMax(BigDecimal requiredTempMax) {
        this.requiredTempMax = requiredTempMax;
    }

    public Integer getRetentionPolicyId() {
        return retentionPolicyId;
    }

    public void setRetentionPolicyId(Integer retentionPolicyId) {
        this.retentionPolicyId = retentionPolicyId;
    }

    public String getRetentionPolicyName() {
        return retentionPolicyName;
    }

    public void setRetentionPolicyName(String retentionPolicyName) {
        this.retentionPolicyName = retentionPolicyName;
    }

    public Date getRetentionExpiryDate() {
        return retentionExpiryDate;
    }

    public void setRetentionExpiryDate(Date retentionExpiryDate) {
        this.retentionExpiryDate = retentionExpiryDate;
    }

    public String getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(String workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    /**
     * Nested DTO for sample type information.
     */
    public static class SampleTypeDTO {
        private String id;
        private String description;

        public SampleTypeDTO() {
        }

        public SampleTypeDTO(String id, String description) {
            this.id = id;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
