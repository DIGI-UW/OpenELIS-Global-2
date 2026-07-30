package org.openelisglobal.qc.valueholder;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * QCControlLot represents a specific batch of quality control material with
 * defined statistical characteristics used for instrument QC evaluation.
 */
@Entity
@Table(name = "qc_control_lot")
public class QCControlLot extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "fhir_uuid", columnDefinition = "uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @NotNull
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @NotNull
    @Column(name = "lot_number", nullable = false, length = 100)
    private String lotNumber;

    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

    @NotNull
    @Column(name = "control_level", nullable = false, length = 50)
    private String controlLevel;

    // testId and instrumentId reference Test.id and Analyzer.id, which are
    // both `String` in Java bridged to a NUMERIC(10,0) SQL column via
    // LIMSStringNumberUserType. Match that pattern here so callers don't
    // need Integer.valueOf(parent.getId()) — per PR #3112 (OGC-346).
    @NotNull
    @Column(name = "test_id", nullable = false)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;

    @NotNull
    @Column(name = "instrument_id", nullable = false)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String instrumentId;

    @NotNull
    @Column(name = "calculation_method", nullable = false, length = 50)
    private String calculationMethod = "INITIAL_RUNS";

    @Column(name = "initial_runs_count")
    private Integer initialRunsCount = 20;

    @Column(name = "manufacturer_mean", precision = 15, scale = 5)
    private Double manufacturerMean;

    @Column(name = "manufacturer_std_dev", precision = 15, scale = 5)
    private Double manufacturerStdDev;

    @Column(name = "activation_date")
    private Timestamp activationDate;

    @Column(name = "expiration_date")
    private Timestamp expirationDate;

    // TODO: Use an Enum instead of a hardcoded string
    @NotNull
    @Column(name = "status", nullable = false, length = 50)
    private String status = "ESTABLISHMENT";

    @Column(name = "sys_user_id", nullable = false)
    private Integer systemUserId;

    @PrePersist
    protected void onCreate() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }

    public QCControlLot() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getControlLevel() {
        return controlLevel;
    }

    public void setControlLevel(String controlLevel) {
        this.controlLevel = controlLevel;
    }

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

    public String getCalculationMethod() {
        return calculationMethod;
    }

    public void setCalculationMethod(String calculationMethod) {
        this.calculationMethod = calculationMethod;
    }

    public Integer getInitialRunsCount() {
        return initialRunsCount;
    }

    public void setInitialRunsCount(Integer initialRunsCount) {
        this.initialRunsCount = initialRunsCount;
    }

    public Double getManufacturerMean() {
        return manufacturerMean;
    }

    public void setManufacturerMean(Double manufacturerMean) {
        this.manufacturerMean = manufacturerMean;
    }

    public Double getManufacturerStdDev() {
        return manufacturerStdDev;
    }

    public void setManufacturerStdDev(Double manufacturerStdDev) {
        this.manufacturerStdDev = manufacturerStdDev;
    }

    public Timestamp getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(Timestamp activationDate) {
        this.activationDate = activationDate;
    }

    public Timestamp getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Timestamp expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }
}
