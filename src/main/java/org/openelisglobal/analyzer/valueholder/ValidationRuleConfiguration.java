package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * ValidationRuleConfiguration entity - Stores configurable validation rules for
 * custom field types.
 * 
 * Per FR-018: Custom field types MUST include validation rules (e.g., format
 * patterns, value ranges, allowed characters) and MUST be available for use in
 * field mapping configuration.
 * 
 */
@Entity
@Table(name = "validation_rule_configuration")
public class ValidationRuleConfiguration extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne
    @JoinColumn(name = "custom_field_type_id", nullable = false)
    @NotNull
    private CustomFieldType customFieldType;

    @Column(name = "rule_name", nullable = false, length = 100)
    @NotNull
    @Size(min = 1, max = 100)
    private String ruleName;

    @Column(name = "rule_type", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private RuleType ruleType;

    @Column(name = "rule_expression", columnDefinition = "TEXT")
    private String ruleExpression; // JSON or text format depending on rule type

    @Column(name = "error_message", length = 500)
    @Size(max = 500)
    private String errorMessage;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    public enum RuleType {
        REGEX, // Regular expression pattern matching
        RANGE, // Numeric range validation (min/max)
        ENUM, // Enumeration of allowed values
        LENGTH // String length validation (minLength/maxLength)
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public CustomFieldType getCustomFieldType() {
        return customFieldType;
    }

    public void setCustomFieldType(CustomFieldType customFieldType) {
        this.customFieldType = customFieldType;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public String getRuleExpression() {
        return ruleExpression;
    }

    public void setRuleExpression(String ruleExpression) {
        this.ruleExpression = ruleExpression;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
