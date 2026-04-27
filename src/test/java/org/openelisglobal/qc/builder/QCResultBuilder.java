package org.openelisglobal.qc.builder;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.qc.valueholder.QCResult;

/**
 * Builder for creating QCResult test data using fluent interface.
 */
public class QCResultBuilder {

    private final QCResult result;

    private QCResultBuilder() {
        result = new QCResult();
        // Set sensible defaults
        result.setId(UUID.randomUUID().toString());
        result.setControlLotId("default-lot-id");
        result.setTestId(1);
        result.setInstrumentId(1);
        result.setResultValue(new BigDecimal("100.0"));
        result.setUnitOfMeasure("mg/dL");
        result.setRunDateTime(new Timestamp(System.currentTimeMillis()));
        result.setResultStatus("PENDING");
        result.setNonConformityFlag(false);
    }

    public static QCResultBuilder create() {
        return new QCResultBuilder();
    }

    public QCResultBuilder withId(String id) {
        result.setId(id);
        return this;
    }

    public QCResultBuilder withControlLotId(String controlLotId) {
        result.setControlLotId(controlLotId);
        return this;
    }

    public QCResultBuilder withTestId(Integer testId) {
        result.setTestId(testId);
        return this;
    }

    public QCResultBuilder withInstrumentId(Integer instrumentId) {
        result.setInstrumentId(instrumentId);
        return this;
    }

    public QCResultBuilder withResultValue(BigDecimal resultValue) {
        result.setResultValue(resultValue);
        return this;
    }

    public QCResultBuilder withResultValue(String value) {
        result.setResultValue(new BigDecimal(value));
        return this;
    }

    public QCResultBuilder withUnitOfMeasure(String unitOfMeasure) {
        result.setUnitOfMeasure(unitOfMeasure);
        return this;
    }

    public QCResultBuilder withZScore(BigDecimal zScore) {
        result.setZScore(zScore);
        return this;
    }

    public QCResultBuilder withRunDateTime(Timestamp runDateTime) {
        result.setRunDateTime(runDateTime);
        return this;
    }

    public QCResultBuilder withTechnicianId(Integer technicianId) {
        result.setTechnicianId(technicianId);
        return this;
    }

    public QCResultBuilder withResultStatus(String resultStatus) {
        result.setResultStatus(resultStatus);
        return this;
    }

    public QCResultBuilder withNonConformityFlag(Boolean nonConformityFlag) {
        result.setNonConformityFlag(nonConformityFlag);
        return this;
    }

    public QCResultBuilder withExternalNotes(String externalNotes) {
        result.setExternalNotes(externalNotes);
        return this;
    }

    public QCResultBuilder asPending() {
        result.setResultStatus("PENDING");
        return this;
    }

    public QCResultBuilder asEvaluated() {
        result.setResultStatus("EVALUATED");
        return this;
    }

    public QCResultBuilder asNonConforming() {
        result.setNonConformityFlag(true);
        return this;
    }

    public QCResult build() {
        return result;
    }
}
