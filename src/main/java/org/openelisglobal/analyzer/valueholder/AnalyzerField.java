package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Represents a specific field or code emitted by an analyzer (e.g., test code,
 * measurement ID, qualifier field) that can be mapped to OpenELIS concepts.
 * Uses UUID-based primary key with automatic generation via @PrePersist.
 */
@Entity
@Table(name = "analyzer_field")
public class AnalyzerField extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_id", nullable = false)
    private Analyzer analyzer;

    @Column(name = "field_name", length = 255, nullable = false)
    private String fieldName;

    @Column(name = "astm_ref", length = 50)
    private String astmRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type")
    private FieldType fieldType;

    @Column(name = "unit", length = 50)
    private String unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_field_type_id")
    private CustomFieldType customFieldType;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getAstmRef() {
        return astmRef;
    }

    public void setAstmRef(String astmRef) {
        this.astmRef = astmRef;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public CustomFieldType getCustomFieldType() {
        return customFieldType;
    }

    public void setCustomFieldType(CustomFieldType customFieldType) {
        this.customFieldType = customFieldType;
    }

    /**
     * Get custom field type ID (convenience method)
     * 
     * @return Custom field type ID or null if not set
     */
    public String getCustomFieldTypeId() {
        return customFieldType != null ? customFieldType.getId() : null;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public enum FieldType {
        NUMERIC, QUALITATIVE, CONTROL_TEST, MELTING_POINT, DATE_TIME, TEXT, CUSTOM
    }
}
