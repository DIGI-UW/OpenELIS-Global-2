package org.openelisglobal.analyzer.form;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Form object for UnitMapping entity - used for REST API input validation
 * Following OpenELIS pattern: Form objects for transport, entities for
 * persistence
 */
public class UnitMappingForm {

    private String id;

    @NotBlank(message = "Analyzer field ID is required")
    private String analyzerFieldId;

    @NotBlank(message = "Analyzer unit is required")
    @Size(min = 1, max = 50, message = "Analyzer unit must be between 1 and 50 characters")
    private String analyzerUnit;

    @NotBlank(message = "OpenELIS unit is required")
    @Size(min = 1, max = 50, message = "OpenELIS unit must be between 1 and 50 characters")
    private String openelisUnit;

    @DecimalMin(value = "0.000001", message = "Conversion factor must be greater than 0")
    @DecimalMax(value = "999999.999999", message = "Conversion factor must not exceed 999999.999999")
    private BigDecimal conversionFactor;

    private Boolean rejectIfMismatch = false;

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAnalyzerFieldId() {
        return analyzerFieldId;
    }

    public void setAnalyzerFieldId(String analyzerFieldId) {
        this.analyzerFieldId = analyzerFieldId;
    }

    public String getAnalyzerUnit() {
        return analyzerUnit;
    }

    public void setAnalyzerUnit(String analyzerUnit) {
        this.analyzerUnit = analyzerUnit;
    }

    public String getOpenelisUnit() {
        return openelisUnit;
    }

    public void setOpenelisUnit(String openelisUnit) {
        this.openelisUnit = openelisUnit;
    }

    public BigDecimal getConversionFactor() {
        return conversionFactor;
    }

    public void setConversionFactor(BigDecimal conversionFactor) {
        this.conversionFactor = conversionFactor;
    }

    public Boolean getRejectIfMismatch() {
        return rejectIfMismatch;
    }

    public void setRejectIfMismatch(Boolean rejectIfMismatch) {
        this.rejectIfMismatch = rejectIfMismatch;
    }
}
