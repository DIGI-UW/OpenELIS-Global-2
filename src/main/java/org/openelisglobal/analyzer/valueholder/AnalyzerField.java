package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.PrePersist;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * AnalyzerField entity - Represents a specific field or code emitted by an
 * analyzer (e.g., test code, measurement ID, qualifier field) that can be
 * mapped to OpenELIS concepts.
 * 
 * This entity is mapped via XML (AnalyzerField.hbm.xml) to match the legacy
 * mapping approach used by other analyzer entities.
 */
public class AnalyzerField extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;

    private Analyzer analyzer;

    private String fieldName;

    private String astmRef;

    private FieldType fieldType;

    private String unit;

    private CustomFieldType customFieldType;

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
