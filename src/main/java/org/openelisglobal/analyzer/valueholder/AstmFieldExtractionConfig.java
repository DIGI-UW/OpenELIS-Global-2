package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Per-analyzer ASTM field extraction overrides (FR-017). Key/field_index/
 * component_index per data-model.md.
 */
@Entity
@Table(name = "astm_field_extraction_config")
public class AstmFieldExtractionConfig extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne
    @JoinColumn(name = "analyzer_id", nullable = false)
    @NotNull
    private Analyzer analyzer;

    @Column(name = "key", nullable = false, length = 60)
    @NotNull
    @Size(max = 60)
    private String key;

    @Column(name = "field_index", nullable = false)
    @NotNull
    private Integer fieldIndex;

    @Column(name = "component_index")
    private Integer componentIndex;

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

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getFieldIndex() {
        return fieldIndex;
    }

    public void setFieldIndex(Integer fieldIndex) {
        this.fieldIndex = fieldIndex;
    }

    public Integer getComponentIndex() {
        return componentIndex;
    }

    public void setComponentIndex(Integer componentIndex) {
        this.componentIndex = componentIndex;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
