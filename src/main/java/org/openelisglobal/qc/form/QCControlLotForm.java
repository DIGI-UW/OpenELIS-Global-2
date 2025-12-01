package org.openelisglobal.qc.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.sql.Timestamp;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form for QC Control Lot data transfer between controller and view. Supports
 * User Story 6: Manage QC Control Lots
 */
public class QCControlLotForm extends BaseForm {

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String id = "";

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String productName = "";

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String lotNumber = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String manufacturer = "";

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String controlLevel = "";

    @NotNull
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String testId = "";

    @NotNull
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String instrumentId = "";

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String calculationMethod = "INITIAL_RUNS";

    @Positive
    private Integer initialRunsCount;

    private Double manufacturerMean;

    private Double manufacturerStdDev;

    @NotNull
    private Timestamp activationDate;

    private Timestamp expirationDate;

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String status = "ESTABLISHMENT";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String unitOfMeasure = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String internalNotes = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String externalNotes = "";

    @Valid
    private QCControlLot controlLot;

    private Timestamp lastupdated;

    public QCControlLotForm() {
        setFormName("qcControlLotForm");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getInternalNotes() {
        return internalNotes;
    }

    public void setInternalNotes(String internalNotes) {
        this.internalNotes = internalNotes;
    }

    public String getExternalNotes() {
        return externalNotes;
    }

    public void setExternalNotes(String externalNotes) {
        this.externalNotes = externalNotes;
    }

    public QCControlLot getControlLot() {
        return controlLot;
    }

    public void setControlLot(QCControlLot controlLot) {
        this.controlLot = controlLot;
    }

    public Timestamp getLastupdated() {
        return lastupdated;
    }

    public void setLastupdated(Timestamp lastupdated) {
        this.lastupdated = lastupdated;
    }
}
