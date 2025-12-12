package org.openelisglobal.analyzer.valueholder;

import java.math.BigDecimal;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * UnitMapping entity - Represents mapping of analyzer-reported units to
 * OpenELIS canonical units, including optional conversion factors for unit
 * mismatches.
 *
 * This entity is mapped via XML (UnitMapping.hbm.xml) to match the legacy
 * mapping approach used by other analyzer entities.
 */
public class UnitMapping extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;

    private String analyzerFieldId;

    // Not persisted; hydrated by services when needed
    private transient AnalyzerField analyzerField;

    private String analyzerUnit;

    private String openelisUnit;

    private BigDecimal conversionFactor;

    private Boolean rejectIfMismatch = false;

    /**
     * Generate UUID if not set. Called by DAO before insert since @PrePersist
     * doesn't work with XML-mapped entities.
     */
    public void generateIdIfNeeded() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getAnalyzerFieldId() {
        return analyzerFieldId;
    }

    public void setAnalyzerFieldId(String analyzerFieldId) {
        this.analyzerFieldId = analyzerFieldId;
    }

    public AnalyzerField getAnalyzerField() {
        return analyzerField;
    }

    public void setAnalyzerField(AnalyzerField analyzerField) {
        this.analyzerField = analyzerField;
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
