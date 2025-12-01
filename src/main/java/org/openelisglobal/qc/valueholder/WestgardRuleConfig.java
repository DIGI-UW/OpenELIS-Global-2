package org.openelisglobal.qc.valueholder;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * WestgardRuleConfig represents the configuration of a specific Westgard rule
 * for a test-instrument combination.
 */
@Entity
@Table(name = "westgard_rule_config", uniqueConstraints = @UniqueConstraint(columnNames = { "test_id", "instrument_id",
        "rule_code" }))
public class WestgardRuleConfig extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @NotNull
    @Column(name = "test_id", nullable = false)
    private Integer testId;

    @NotNull
    @Column(name = "instrument_id", nullable = false)
    private Integer instrumentId;

    @NotNull
    @Column(name = "rule_code", nullable = false, length = 20)
    private String ruleCode;

    @NotNull
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @NotNull
    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @NotNull
    @Column(name = "requires_corrective_action", nullable = false)
    private Boolean requiresCorrectiveAction = false;

    public WestgardRuleConfig() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
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
