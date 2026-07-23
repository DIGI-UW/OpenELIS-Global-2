package org.openelisglobal.qc.dto;

/**
 * Pairs a Westgard rule code with its severity for dashboard display.
 *
 * // TODO: Replace String severity with a shared QCSeverity enum across QC
 * entities
 */
public class TriggeredRuleDetail {

    private String ruleCode;
    private String severity; // "WARNING" or "REJECTION"

    public TriggeredRuleDetail() {
    }

    public TriggeredRuleDetail(String ruleCode, String severity) {
        this.ruleCode = ruleCode;
        this.severity = severity;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}