package org.openelisglobal.qc.dto;

import java.util.List;

/**
 * DTO summarising the Westgard rule configuration for one (test, instrument)
 * combination. Returned by the GET /rest/qc/ruleConfig/summaries endpoint.
 */
public class RuleConfigSummary {

    private String testId;
    private String instrumentId;
    private String testName;
    private String instrumentName;
    private int enabledRuleCount;
    private int totalRuleCount;
    private List<RuleConfigDetail> rules;

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getInstrumentName() {
        return instrumentName;
    }

    public void setInstrumentName(String instrumentName) {
        this.instrumentName = instrumentName;
    }

    public int getEnabledRuleCount() {
        return enabledRuleCount;
    }

    public void setEnabledRuleCount(int enabledRuleCount) {
        this.enabledRuleCount = enabledRuleCount;
    }

    public int getTotalRuleCount() {
        return totalRuleCount;
    }

    public void setTotalRuleCount(int totalRuleCount) {
        this.totalRuleCount = totalRuleCount;
    }

    public List<RuleConfigDetail> getRules() {
        return rules;
    }

    public void setRules(List<RuleConfigDetail> rules) {
        this.rules = rules;
    }

    /**
     * Detail of a single rule config within the summary.
     */
    public static class RuleConfigDetail {

        private String id;
        private String ruleCode;
        private boolean enabled;
        private String severity;

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

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }
    }
}
