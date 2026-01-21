package org.openelisglobal.biorepository.controller.rest.dto;

import java.math.BigDecimal;

/**
 * DTO for sample registration requests from the frontend. Maps frontend field
 * names to backend entity fields.
 */
public class SampleRegistrationDTO {

    // Required fields per SRS
    private String originLab; // Originating laboratory/source
    private String barcode; // Sample ID (unique barcode)
    private String sampleType; // Sample type (name or ID)
    private String receiptDate; // Date/time of receipt (ISO string)
    private BigDecimal requiredTempMin; // Storage temp min
    private BigDecimal requiredTempMax; // Storage temp max

    // Conditional/Optional fields
    private String externalId; // External/Donor ID
    private String projectId; // Project ID
    private String principalInvestigator; // PI name
    private String consentId; // Consent ID (required for human samples)
    private String ethicsApprovalRef; // Ethics approval reference
    private String mtaReference; // MTA reference
    private String biosafetyLevel; // BSL_1, BSL_2, etc.
    private String preservationMedium; // Preservation method (EDTA, Heparin, etc.)
    private String arrivalCondition; // Condition at receipt (Good, Damaged, etc.)
    private String specialHandling; // Special handling instructions
    private String collectionDate; // Original collection date
    private Integer shipmentId; // Associated shipment

    // Getters and setters
    public String getOriginLab() {
        return originLab;
    }

    public void setOriginLab(String originLab) {
        this.originLab = originLab;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    /**
     * Alias for getSampleType() for compatibility with validation endpoint.
     */
    public String getSampleTypeId() {
        return sampleType;
    }

    /**
     * Alias for setSampleType() for compatibility.
     */
    public void setSampleTypeId(String sampleTypeId) {
        this.sampleType = sampleTypeId;
    }

    public String getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(String receiptDate) {
        this.receiptDate = receiptDate;
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

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
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

    public String getConsentId() {
        return consentId;
    }

    public void setConsentId(String consentId) {
        this.consentId = consentId;
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

    public String getBiosafetyLevel() {
        return biosafetyLevel;
    }

    public void setBiosafetyLevel(String biosafetyLevel) {
        this.biosafetyLevel = biosafetyLevel;
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

    public String getSpecialHandling() {
        return specialHandling;
    }

    public void setSpecialHandling(String specialHandling) {
        this.specialHandling = specialHandling;
    }

    public String getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(String collectionDate) {
        this.collectionDate = collectionDate;
    }

    public Integer getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Integer shipmentId) {
        this.shipmentId = shipmentId;
    }
}
