package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * UnitMapping entity - Represents mapping of analyzer-reported units to
 * OpenELIS canonical units, including optional conversion factors for unit
 * mismatches.
 */
@Entity
@Table(name = "unit_mapping")
public class UnitMapping extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "analyzer_field_id", nullable = false, length = 36)
    private String analyzerFieldId;

    // Not persisted; hydrated by services when needed
    private transient AnalyzerField analyzerField;

    @Column(name = "analyzer_unit", nullable = false, length = 50)
    private String analyzerUnit;

    @Column(name = "openelis_unit", nullable = false, length = 50)
    private String openelisUnit;

    @Column(name = "conversion_factor", precision = 10, scale = 6)
    private BigDecimal conversionFactor;

    @Column(name = "reject_if_mismatch")
    private Boolean rejectIfMismatch = false;

    @PrePersist
    public void generateIdIfNeeded() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (getLastupdated() == null) {
            setLastupdatedFields();
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
