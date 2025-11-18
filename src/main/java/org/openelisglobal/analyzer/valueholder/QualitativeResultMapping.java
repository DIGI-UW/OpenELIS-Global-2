package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * QualitativeResultMapping entity - Represents mapping of instrument-specific
 * qualitative values (strings or codes) to canonical OpenELIS-coded results,
 * supporting many-to-one mapping (multiple analyzer values → single OpenELIS code).
 */
@Entity
@Table(name = "qualitative_result_mapping", uniqueConstraints = @UniqueConstraint(columnNames = {
        "analyzer_field_id", "analyzer_value" }))
public class QualitativeResultMapping extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_field_id", nullable = false, referencedColumnName = "id")
    private AnalyzerField analyzerField;

    @Column(name = "analyzer_value", nullable = false, length = 100)
    @NotNull
    @Size(min = 1, max = 100)
    private String analyzerValue;

    @Column(name = "openelis_code", nullable = false, length = 100)
    @NotNull
    @Size(min = 1, max = 100)
    private String openelisCode;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @PrePersist
    protected void onCreate() {
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

