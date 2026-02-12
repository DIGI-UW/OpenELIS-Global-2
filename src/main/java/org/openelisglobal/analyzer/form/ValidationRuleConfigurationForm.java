package org.openelisglobal.analyzer.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Form DTO for ValidationRuleConfiguration entity
 * 
 * Used for request body validation in REST controller endpoints.
 * 
 * Per FR-018: Custom field types MUST include validation rules (e.g., format
 * patterns, value ranges, allowed characters) and MUST be available for use in
 * field mapping configuration.
 * 
 */
public class ValidationRuleConfigurationForm {

    private String id;

    @NotNull(message = "Custom field type ID is required")
    @Size(min = 1, max = 36, message = "Custom field type ID must be between 1 and 36 characters")
    private String customFieldTypeId;

    @NotNull(message = "Rule name is required")
    @Size(min = 1, max = 100, message = "Rule name must be between 1 and 100 characters")
    private String ruleName;

    @NotNull(message = "Rule type is required")
    private String ruleType; // REGEX, RANGE, ENUM, LENGTH

    private String ruleExpression; // JSON or text format depending on rule type

    @Size(max = 500, message = "Error message must not exceed 500 characters")
    private String errorMessage;

    private Boolean isActive;

    public ValidationRuleConfigurationForm() {
        this.isActive = true; // Default to active
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomFieldTypeId() {
        return customFieldTypeId;
    }

    public void setCustomFieldTypeId(String customFieldTypeId) {
        this.customFieldTypeId = customFieldTypeId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
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
