package org.openelisglobal.qc.builder;

import java.util.UUID;
import org.openelisglobal.qc.valueholder.WestgardRuleConfig;

/**
 * Builder for creating WestgardRuleConfig test data using fluent interface.
 */
public class WestgardRuleConfigBuilder {

    private final WestgardRuleConfig ruleConfig;

    private WestgardRuleConfigBuilder() {
        ruleConfig = new WestgardRuleConfig();
        // Set sensible defaults
        ruleConfig.setId(UUID.randomUUID().toString());
        ruleConfig.setTestId(1);
        ruleConfig.setInstrumentId(1);
        ruleConfig.setRuleCode("1_3s");
        ruleConfig.setEnabled(true);
        ruleConfig.setSeverity("WARNING");
        ruleConfig.setRequiresCorrectiveAction(false);
    }

    public static WestgardRuleConfigBuilder create() {
        return new WestgardRuleConfigBuilder();
    }

    public WestgardRuleConfigBuilder withId(String id) {
        ruleConfig.setId(id);
        return this;
    }

    public WestgardRuleConfigBuilder withTestId(Integer testId) {
        ruleConfig.setTestId(testId);
        return this;
    }

    public WestgardRuleConfigBuilder withInstrumentId(Integer instrumentId) {
        ruleConfig.setInstrumentId(instrumentId);
        return this;
    }

    public WestgardRuleConfigBuilder withRuleCode(String ruleCode) {
        ruleConfig.setRuleCode(ruleCode);
        return this;
    }

    public WestgardRuleConfigBuilder withEnabled(Boolean enabled) {
        ruleConfig.setEnabled(enabled);
        return this;
    }

    public WestgardRuleConfigBuilder withSeverity(String severity) {
        ruleConfig.setSeverity(severity);
        return this;
    }

    public WestgardRuleConfigBuilder withRequiresCorrectiveAction(Boolean requiresCorrectiveAction) {
        ruleConfig.setRequiresCorrectiveAction(requiresCorrectiveAction);
        return this;
    }

    public WestgardRuleConfigBuilder asWarning() {
        ruleConfig.setSeverity("WARNING");
        ruleConfig.setRequiresCorrectiveAction(false);
        return this;
    }

    public WestgardRuleConfigBuilder asCritical() {
        ruleConfig.setSeverity("CRITICAL");
        ruleConfig.setRequiresCorrectiveAction(true);
        return this;
    }

    public WestgardRuleConfigBuilder asDisabled() {
        ruleConfig.setEnabled(false);
        return this;
    }

    public WestgardRuleConfigBuilder forRule1_3s() {
        ruleConfig.setRuleCode("1_3s");
        ruleConfig.setSeverity("WARNING");
        return this;
    }

    public WestgardRuleConfigBuilder forRule2_2s() {
        ruleConfig.setRuleCode("2_2s");
        ruleConfig.setSeverity("WARNING");
        return this;
    }

    public WestgardRuleConfigBuilder forRuleR_4s() {
        ruleConfig.setRuleCode("R_4s");
        ruleConfig.setSeverity("CRITICAL");
        ruleConfig.setRequiresCorrectiveAction(true);
        return this;
    }

    public WestgardRuleConfig build() {
        return ruleConfig;
    }
}
