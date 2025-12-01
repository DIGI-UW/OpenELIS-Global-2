package org.openelisglobal.qc.builder;

import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.qc.valueholder.QCRuleViolation;

/**
 * Builder for creating QCRuleViolation test data using fluent interface.
 */
public class QCRuleViolationBuilder {

    private final QCRuleViolation violation;

    private QCRuleViolationBuilder() {
        violation = new QCRuleViolation();
        // Set sensible defaults
        violation.setId(UUID.randomUUID().toString());
        violation.setTriggeringResultId("default-result-id");
        violation.setRuleCode("1_3s");
        violation.setViolationDateTime(new Timestamp(System.currentTimeMillis()));
        violation.setSeverity("WARNING");
        violation.setInstrumentId(1);
        violation.setTestId(1);
        violation.setResolutionStatus("UNRESOLVED");
    }

    public static QCRuleViolationBuilder create() {
        return new QCRuleViolationBuilder();
    }

    public QCRuleViolationBuilder withId(String id) {
        violation.setId(id);
        return this;
    }

    public QCRuleViolationBuilder withTriggeringResultId(String triggeringResultId) {
        violation.setTriggeringResultId(triggeringResultId);
        return this;
    }

    public QCRuleViolationBuilder withRuleCode(String ruleCode) {
        violation.setRuleCode(ruleCode);
        return this;
    }

    public QCRuleViolationBuilder withViolationDateTime(Timestamp violationDateTime) {
        violation.setViolationDateTime(violationDateTime);
        return this;
    }

    public QCRuleViolationBuilder withSeverity(String severity) {
        violation.setSeverity(severity);
        return this;
    }

    public QCRuleViolationBuilder withInstrumentId(Integer instrumentId) {
        violation.setInstrumentId(instrumentId);
        return this;
    }

    public QCRuleViolationBuilder withTestId(Integer testId) {
        violation.setTestId(testId);
        return this;
    }

    public QCRuleViolationBuilder withResolutionStatus(String resolutionStatus) {
        violation.setResolutionStatus(resolutionStatus);
        return this;
    }

    public QCRuleViolationBuilder withResolvedDateTime(Timestamp resolvedDateTime) {
        violation.setResolvedDateTime(resolvedDateTime);
        return this;
    }

    public QCRuleViolationBuilder withResolvedByUserId(Integer resolvedByUserId) {
        violation.setResolvedByUserId(resolvedByUserId);
        return this;
    }

    public QCRuleViolationBuilder withResolutionNotes(String resolutionNotes) {
        violation.setResolutionNotes(resolutionNotes);
        return this;
    }

    public QCRuleViolationBuilder asUnresolved() {
        violation.setResolutionStatus("UNRESOLVED");
        return this;
    }

    public QCRuleViolationBuilder asResolved(Integer resolvedByUserId) {
        violation.setResolutionStatus("RESOLVED");
        violation.setResolvedDateTime(new Timestamp(System.currentTimeMillis()));
        violation.setResolvedByUserId(resolvedByUserId);
        return this;
    }

    public QCRuleViolationBuilder asAcknowledged() {
        violation.setResolutionStatus("ACKNOWLEDGED");
        return this;
    }

    public QCRuleViolationBuilder asWarning() {
        violation.setSeverity("WARNING");
        return this;
    }

    public QCRuleViolationBuilder asCritical() {
        violation.setSeverity("CRITICAL");
        return this;
    }

    public QCRuleViolation build() {
        return violation;
    }
}
