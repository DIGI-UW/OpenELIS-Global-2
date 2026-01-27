package org.openelisglobal.analyzer.valueholder;

import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * QualitativeResultMapping entity - Represents mapping of analyzer-specific
 * qualitative values (strings or codes) to canonical OpenELIS-coded results,
 * supporting many-to-one mapping (multiple analyzer values to single OpenELIS
 * code).
 *
 * This entity is mapped via XML (QualitativeResultMapping.hbm.xml) to match the
 * legacy mapping approach used by other analyzer entities.
 */
public class QualitativeResultMapping extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;

    private String analyzerFieldId;

    // Not persisted; hydrated by services when needed
    private transient AnalyzerField analyzerField;

    private String analyzerValue;

    private String openelisCode;

    private Boolean isDefault = false;

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

    public String getAnalyzerValue() {
        return analyzerValue;
    }

    public void setAnalyzerValue(String analyzerValue) {
        this.analyzerValue = analyzerValue;
    }

    public String getOpenelisCode() {
        return openelisCode;
    }

    public void setOpenelisCode(String openelisCode) {
        this.openelisCode = openelisCode;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
