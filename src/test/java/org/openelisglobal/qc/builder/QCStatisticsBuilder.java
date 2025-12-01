package org.openelisglobal.qc.builder;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.qc.valueholder.QCStatistics;

/**
 * Builder for creating QCStatistics test data using fluent interface.
 */
public class QCStatisticsBuilder {

    private final QCStatistics statistics;

    private QCStatisticsBuilder() {
        statistics = new QCStatistics();
        // Set sensible defaults
        statistics.setId(UUID.randomUUID().toString());
        statistics.setControlLotId("default-lot-id");
        statistics.setCalculationDate(new Timestamp(System.currentTimeMillis()));
        statistics.setMean(new BigDecimal("100.0"));
        statistics.setStandardDeviation(new BigDecimal("5.0"));
        statistics.setNumValues(20);
        statistics.setCalculationMethod("INITIAL_RUNS");
        statistics.setValidityStart(new Timestamp(System.currentTimeMillis()));
    }

    public static QCStatisticsBuilder create() {
        return new QCStatisticsBuilder();
    }

    public QCStatisticsBuilder withId(String id) {
        statistics.setId(id);
        return this;
    }

    public QCStatisticsBuilder withControlLotId(String controlLotId) {
        statistics.setControlLotId(controlLotId);
        return this;
    }

    public QCStatisticsBuilder withCalculationDate(Timestamp calculationDate) {
        statistics.setCalculationDate(calculationDate);
        return this;
    }

    public QCStatisticsBuilder withMean(BigDecimal mean) {
        statistics.setMean(mean);
        return this;
    }

    public QCStatisticsBuilder withMean(String mean) {
        statistics.setMean(new BigDecimal(mean));
        return this;
    }

    public QCStatisticsBuilder withStandardDeviation(BigDecimal standardDeviation) {
        statistics.setStandardDeviation(standardDeviation);
        return this;
    }

    public QCStatisticsBuilder withStandardDeviation(String stdDev) {
        statistics.setStandardDeviation(new BigDecimal(stdDev));
        return this;
    }

    public QCStatisticsBuilder withNumValues(Integer numValues) {
        statistics.setNumValues(numValues);
        return this;
    }

    public QCStatisticsBuilder withCalculationMethod(String calculationMethod) {
        statistics.setCalculationMethod(calculationMethod);
        return this;
    }

    public QCStatisticsBuilder withValidityStart(Timestamp validityStart) {
        statistics.setValidityStart(validityStart);
        return this;
    }

    public QCStatisticsBuilder withValidityEnd(Timestamp validityEnd) {
        statistics.setValidityEnd(validityEnd);
        return this;
    }

    public QCStatisticsBuilder asInitialRuns(int numRuns) {
        statistics.setCalculationMethod("INITIAL_RUNS");
        statistics.setNumValues(numRuns);
        return this;
    }

    public QCStatisticsBuilder asRollingWindow() {
        statistics.setCalculationMethod("ROLLING");
        return this;
    }

    public QCStatisticsBuilder asManufacturerFixed() {
        statistics.setCalculationMethod("MANUFACTURER_FIXED");
        return this;
    }

    public QCStatistics build() {
        return statistics;
    }
}
