package org.openelisglobal.qc.service.evaluator;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a Westgard rule evaluation (T088)
 *
 * Contains the evaluation outcome including whether a violation occurred, the
 * severity level, affected results, and a descriptive message.
 */
public class RuleEvaluationResult {

    private final String ruleCode;
    private final boolean violated;
    private final String severity;
    private final List<String> affectedResultIds;
    private final String message;
    private final boolean evaluated;

    private RuleEvaluationResult(Builder builder) {
        this.ruleCode = builder.ruleCode;
        this.violated = builder.violated;
        this.severity = builder.severity;
        this.affectedResultIds = builder.affectedResultIds;
        this.message = builder.message;
        this.evaluated = builder.evaluated;
    }

    /**
     * Create a result indicating the rule was violated.
     */
    public static RuleEvaluationResult violation(String ruleCode, String severity, List<String> affectedResultIds,
            String message) {
        return new Builder(ruleCode).violated(true).severity(severity).affectedResultIds(affectedResultIds)
                .message(message).evaluated(true).build();
    }

    /**
     * Create a result indicating no violation was detected.
     */
    public static RuleEvaluationResult noViolation(String ruleCode) {
        return new Builder(ruleCode).violated(false).evaluated(true).message("No violation detected").build();
    }

    /**
     * Create a result indicating the rule could not be evaluated (insufficient
     * data).
     */
    public static RuleEvaluationResult cannotEvaluate(String ruleCode, String reason) {
        return new Builder(ruleCode).violated(false).evaluated(false).message(reason).build();
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public boolean isViolated() {
        return violated;
    }

    public String getSeverity() {
        return severity;
    }

    public List<String> getAffectedResultIds() {
        return affectedResultIds != null ? affectedResultIds : new ArrayList<>();
    }

    public String getMessage() {
        return message;
    }

    public boolean isEvaluated() {
        return evaluated;
    }

    @Override
    public String toString() {
        return "RuleEvaluationResult{" + "ruleCode='" + ruleCode + '\'' + ", violated=" + violated + ", severity='"
                + severity + '\'' + ", evaluated=" + evaluated + ", message='" + message + '\'' + '}';
    }

    /**
     * Builder for RuleEvaluationResult
     */
    public static class Builder {

        private final String ruleCode;
        private boolean violated = false;
        private String severity;
        private List<String> affectedResultIds = new ArrayList<>();
        private String message;
        private boolean evaluated = true;

        public Builder(String ruleCode) {
            this.ruleCode = ruleCode;
        }

        public Builder violated(boolean violated) {
            this.violated = violated;
            return this;
        }

        public Builder severity(String severity) {
            this.severity = severity;
            return this;
        }

        public Builder affectedResultIds(List<String> affectedResultIds) {
            this.affectedResultIds = affectedResultIds;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder evaluated(boolean evaluated) {
            this.evaluated = evaluated;
            return this;
        }

        public RuleEvaluationResult build() {
            return new RuleEvaluationResult(this);
        }
    }
}
