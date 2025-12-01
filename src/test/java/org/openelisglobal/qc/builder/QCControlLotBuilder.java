package org.openelisglobal.qc.builder;

import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.qc.valueholder.QCControlLot;

/**
 * Builder for creating QCControlLot test data using fluent interface.
 * Eliminates hardcoded test values and enables readable test setup.
 */
public class QCControlLotBuilder {

    private final QCControlLot controlLot;

    private QCControlLotBuilder() {
        controlLot = new QCControlLot();
        // Set sensible defaults
        controlLot.setId(UUID.randomUUID().toString());
        controlLot.setProductName("Test Control Material");
        controlLot.setLotNumber("LOT-" + System.currentTimeMillis());
        controlLot.setManufacturer("Test Manufacturer");
        controlLot.setControlLevel("LEVEL_1");
        controlLot.setTestId(1);
        controlLot.setInstrumentId(1);
        controlLot.setCalculationMethod("INITIAL_RUNS");
        controlLot.setInitialRunsCount(20);
        controlLot.setActivationDate(new Timestamp(System.currentTimeMillis()));
        controlLot.setStatus("ESTABLISHMENT");
    }

    public static QCControlLotBuilder create() {
        return new QCControlLotBuilder();
    }

    public QCControlLotBuilder withId(String id) {
        controlLot.setId(id);
        return this;
    }

    public QCControlLotBuilder withProductName(String productName) {
        controlLot.setProductName(productName);
        return this;
    }

    public QCControlLotBuilder withLotNumber(String lotNumber) {
        controlLot.setLotNumber(lotNumber);
        return this;
    }

    public QCControlLotBuilder withManufacturer(String manufacturer) {
        controlLot.setManufacturer(manufacturer);
        return this;
    }

    public QCControlLotBuilder withControlLevel(String controlLevel) {
        controlLot.setControlLevel(controlLevel);
        return this;
    }

    public QCControlLotBuilder withTestId(Integer testId) {
        controlLot.setTestId(testId);
        return this;
    }

    public QCControlLotBuilder withInstrumentId(Integer instrumentId) {
        controlLot.setInstrumentId(instrumentId);
        return this;
    }

    public QCControlLotBuilder withCalculationMethod(String calculationMethod) {
        controlLot.setCalculationMethod(calculationMethod);
        return this;
    }

    public QCControlLotBuilder withInitialRunsCount(Integer initialRunsCount) {
        controlLot.setInitialRunsCount(initialRunsCount);
        return this;
    }

    public QCControlLotBuilder withManufacturerMean(Double mean) {
        controlLot.setManufacturerMean(mean);
        return this;
    }

    public QCControlLotBuilder withManufacturerStdDev(Double stdDev) {
        controlLot.setManufacturerStdDev(stdDev);
        return this;
    }

    public QCControlLotBuilder withActivationDate(Timestamp activationDate) {
        controlLot.setActivationDate(activationDate);
        return this;
    }

    public QCControlLotBuilder withExpirationDate(Timestamp expirationDate) {
        controlLot.setExpirationDate(expirationDate);
        return this;
    }

    public QCControlLotBuilder withStatus(String status) {
        controlLot.setStatus(status);
        return this;
    }

    public QCControlLotBuilder asActive() {
        controlLot.setStatus("ACTIVE");
        return this;
    }

    public QCControlLotBuilder asExpired() {
        controlLot.setStatus("EXPIRED");
        return this;
    }

    public QCControlLotBuilder withManufacturerValues(Double mean, Double stdDev) {
        controlLot.setManufacturerMean(mean);
        controlLot.setManufacturerStdDev(stdDev);
        controlLot.setCalculationMethod("MANUFACTURER_FIXED");
        return this;
    }

    public QCControlLot build() {
        return controlLot;
    }
}
