package org.openelisglobal.qc.form;

import java.util.List;
import javax.validation.constraints.NotNull;
import org.openelisglobal.common.form.BaseForm;

/**
 * Form DTO for Westgard Rule Configuration (T072)
 *
 * Supports User Story 5: Configure Westgard Rules Following Constitution IV.5:
 * Forms/DTOs for client-server communication
 */
public class WestgardRuleConfigForm extends BaseForm {

    private static final long serialVersionUID = 1L;

    // Test and Instrument identifiers
    @NotNull(message = "Test ID is required")
    private Integer testId;

    @NotNull(message = "Instrument ID is required")
    private Integer instrumentId;

    // Rule configurations
    private List<RuleConfigDTO> ruleConfigs;

    // Preset to apply (BASIC, STANDARD, COMPREHENSIVE)
    private String preset;

    // Individual rule update
    private RuleConfigDTO ruleConfig;

    public WestgardRuleConfigForm() {
        // Default constructor
    }

    public Integer getTestId() {
        return testId;
    }

    public void setTestId(Integer testId) {
        this.testId = testId;
    }

    public Integer getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(Integer instrumentId) {
        this.instrumentId = instrumentId;
    }

    public List<RuleConfigDTO> getRuleConfigs() {
        return ruleConfigs;
    }

    public void setRuleConfigs(List<RuleConfigDTO> ruleConfigs) {
        this.ruleConfigs = ruleConfigs;
    }

    public String getPreset() {
        return preset;
    }

    public void setPreset(String preset) {
        this.preset = preset;
    }

    public RuleConfigDTO getRuleConfig() {
        return ruleConfig;
    }

    public void setRuleConfig(RuleConfigDTO ruleConfig) {
        this.ruleConfig = ruleConfig;
    }

    /**
     * DTO for individual rule configuration
     */
    public static class RuleConfigDTO {

        private String id;

        @NotNull(message = "Rule code is required")
        private String ruleCode;

        private Boolean enabled;
        private String severity;
        private Boolean requiresCorrectiveAction;

        public RuleConfigDTO() {
            // Default constructor
        }

        public RuleConfigDTO(String id, String ruleCode, Boolean enabled, String severity,
                Boolean requiresCorrectiveAction) {
            this.id = id;
            this.ruleCode = ruleCode;
            this.enabled = enabled;
            this.severity = severity;
            this.requiresCorrectiveAction = requiresCorrectiveAction;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getRuleCode() {
            return ruleCode;
        }

        public void setRuleCode(String ruleCode) {
            this.ruleCode = ruleCode;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public Boolean getRequiresCorrectiveAction() {
            return requiresCorrectiveAction;
        }

        public void setRequiresCorrectiveAction(Boolean requiresCorrectiveAction) {
            this.requiresCorrectiveAction = requiresCorrectiveAction;
        }
    }
}
