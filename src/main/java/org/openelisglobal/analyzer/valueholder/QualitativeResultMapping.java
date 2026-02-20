package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * QualitativeResultMapping entity - Represents mapping of analyzer-specific
 * qualitative values (strings or codes) to canonical OpenELIS-coded results,
 * supporting many-to-one mapping (multiple analyzer values to single OpenELIS
 * code).
 */
@Entity
@Table(name = "qualitative_result_mapping")
public class QualitativeResultMapping extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "analyzer_field_id", nullable = false, length = 36)
    private String analyzerFieldId;

    // Not persisted; hydrated by services when needed
    private transient AnalyzerField analyzerField;

    @Column(name = "analyzer_value", nullable = false, length = 100)
    private String analyzerValue;

    @Column(name = "openelis_code", nullable = false, length = 100)
    private String openelisCode;

    @Column(name = "is_default")
    private Boolean isDefault = false;

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
