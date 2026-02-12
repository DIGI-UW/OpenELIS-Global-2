package org.openelisglobal.analyzer.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Form object for creating new OpenELIS fields inline from the analyzer mapping
 * interface.
 * 
 * Supports 8 entity types: TEST, PANEL, RESULT, ORDER, SAMPLE, QC, METADATA,
 * UNIT
 */
public class OpenELISFieldForm {

    @NotNull(message = "Entity type is required")
    private EntityType entityType;

    // Common fields
    @NotBlank(message = "Field name is required")
    @Size(min = 1, max = 200, message = "Field name must be between 1 and 200 characters")
    private String fieldName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Pattern(regexp = "\\d{4,5}-\\d", message = "LOINC code must match format NNNNN-N")
    private String loincCode;

    // TEST-specific fields
    @Size(min = 1, max = 50, message = "Test code must be between 1 and 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Test code must contain only alphanumeric characters, hyphens, and underscores")
    private String testCode;

    private String sampleTypeId;
    private String resultType; // NUMERIC, QUALITATIVE, TEXT

    // PANEL-specific fields
    @Size(min = 1, max = 50, message = "Panel code must be between 1 and 50 characters")
    private String panelCode;
    private List<String> memberTestIds;

    // RESULT-specific fields
    private String analyteId;
    private String resultGroup;
    private Integer reportingSequence;

    // ORDER-specific fields
    private String orderType; // ROUTINE, URGENT, STAT, PRIORITY
    private Integer priority; // 1-10

    // SAMPLE-specific fields
    @Size(min = 1, max = 50, message = "Sample type code must be between 1 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Sample type code must be uppercase letters, numbers, or hyphens only")
    private String sampleTypeCode;
    @Size(min = 1, max = 100, message = "Sample type name must be between 1 and 100 characters")
    private String sampleTypeName;
    private String containerType;
    private String collectionMethod;

    // QC-specific fields
    @Size(min = 1, max = 100, message = "Control name must be between 1 and 100 characters")
    private String controlName;
    @Size(min = 1, max = 50, message = "Lot number must be between 1 and 50 characters")
    private String lotNumber;
    private String expirationDate;
    private Double targetRangeMin;
    private Double targetRangeMax;

    // METADATA-specific fields
    private String dataType; // STRING, NUMBER, DATE, BOOLEAN
    private String formatPattern;

    // UNIT-specific fields
    @Size(min = 1, max = 20, message = "Unit code must be between 1 and 20 characters")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Unit code must be uppercase letters, numbers, or hyphens only")
    private String unitCode;
    @Size(min = 1, max = 50, message = "Unit name must be between 1 and 50 characters")
    private String unitName;
    private String siEquivalent;
    private Double conversionFactor;

    // Field type compatibility
    private String fieldType; // NUMERIC, QUALITATIVE, TEXT, CUSTOM
    private List<String> acceptedUnits;

    public enum EntityType {
        TEST, PANEL, RESULT, ORDER, SAMPLE, QC, METADATA, UNIT
    }

    // Getters and setters
    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLoincCode() {
        return loincCode;
    }

    public void setLoincCode(String loincCode) {
        this.loincCode = loincCode;
    }

    public String getTestCode() {
        return testCode;
    }

    public void setTestCode(String testCode) {
        this.testCode = testCode;
    }

    public String getSampleTypeId() {
        return sampleTypeId;
    }

    public void setSampleTypeId(String sampleTypeId) {
        this.sampleTypeId = sampleTypeId;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getPanelCode() {
        return panelCode;
    }

    public void setPanelCode(String panelCode) {
        this.panelCode = panelCode;
    }

    public List<String> getMemberTestIds() {
        return memberTestIds;
    }

    public void setMemberTestIds(List<String> memberTestIds) {
        this.memberTestIds = memberTestIds;
    }

    public String getAnalyteId() {
        return analyteId;
    }

    public void setAnalyteId(String analyteId) {
        this.analyteId = analyteId;
    }

    public String getResultGroup() {
        return resultGroup;
    }

    public void setResultGroup(String resultGroup) {
        this.resultGroup = resultGroup;
    }

    public Integer getReportingSequence() {
        return reportingSequence;
    }

    public void setReportingSequence(Integer reportingSequence) {
        this.reportingSequence = reportingSequence;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getSampleTypeCode() {
        return sampleTypeCode;
    }

    public void setSampleTypeCode(String sampleTypeCode) {
        this.sampleTypeCode = sampleTypeCode;
    }

    public String getSampleTypeName() {
        return sampleTypeName;
    }

    public void setSampleTypeName(String sampleTypeName) {
        this.sampleTypeName = sampleTypeName;
    }

    public String getContainerType() {
        return containerType;
    }

    public void setContainerType(String containerType) {
        this.containerType = containerType;
    }

    public String getCollectionMethod() {
        return collectionMethod;
    }

    public void setCollectionMethod(String collectionMethod) {
        this.collectionMethod = collectionMethod;
    }

    public String getControlName() {
        return controlName;
    }

    public void setControlName(String controlName) {
        this.controlName = controlName;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Double getTargetRangeMin() {
        return targetRangeMin;
    }

    public void setTargetRangeMin(Double targetRangeMin) {
        this.targetRangeMin = targetRangeMin;
    }

    public Double getTargetRangeMax() {
        return targetRangeMax;
    }

    public void setTargetRangeMax(Double targetRangeMax) {
        this.targetRangeMax = targetRangeMax;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getFormatPattern() {
        return formatPattern;
    }

    public void setFormatPattern(String formatPattern) {
        this.formatPattern = formatPattern;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getSiEquivalent() {
        return siEquivalent;
    }

    public void setSiEquivalent(String siEquivalent) {
        this.siEquivalent = siEquivalent;
    }

    public Double getConversionFactor() {
        return conversionFactor;
    }

    public void setConversionFactor(Double conversionFactor) {
        this.conversionFactor = conversionFactor;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public List<String> getAcceptedUnits() {
        return acceptedUnits;
    }

    public void setAcceptedUnits(List<String> acceptedUnits) {
        this.acceptedUnits = acceptedUnits;
    }
}
