package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * CustomFieldType entity - Allows system administrators to add custom field
 * types with validation rules, extending beyond the standard field types.
 * 
 * Per FR-018: Custom field types MUST include validation rules (e.g., format
 * patterns, value ranges, allowed characters) and MUST be available for use in
 * field mapping configuration.
 */
@Entity
@Table(name = "custom_field_type")
public class CustomFieldType extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "type_name", nullable = false, length = 50, unique = true)
    @NotNull
    @Size(min = 1, max = 50)
    private String typeName;

    @Column(name = "display_name", nullable = false, length = 100)
    @NotNull
    @Size(min = 1, max = 100)
    private String displayName;

    @Column(name = "validation_pattern", length = 255)
    private String validationPattern; // Regex pattern

    @Column(name = "value_range_min", precision = 10, scale = 2)
    private BigDecimal valueRangeMin; // For numeric types

    @Column(name = "value_range_max", precision = 10, scale = 2)
    private BigDecimal valueRangeMax; // For numeric types

    @Column(name = "allowed_characters", length = 255)
    private String allowedCharacters; // Character set restriction

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

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getValidationPattern() {
        return validationPattern;
    }

    public void setValidationPattern(String validationPattern) {
        this.validationPattern = validationPattern;
    }

    public BigDecimal getValueRangeMin() {
        return valueRangeMin;
    }

    public void setValueRangeMin(BigDecimal valueRangeMin) {
        this.valueRangeMin = valueRangeMin;
    }

    public BigDecimal getValueRangeMax() {
        return valueRangeMax;
    }

    public void setValueRangeMax(BigDecimal valueRangeMax) {
        this.valueRangeMax = valueRangeMax;
    }

    public String getAllowedCharacters() {
        return allowedCharacters;
    }

    public void setAllowedCharacters(String allowedCharacters) {
        this.allowedCharacters = allowedCharacters;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
