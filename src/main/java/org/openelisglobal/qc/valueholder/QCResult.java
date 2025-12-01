package org.openelisglobal.qc.valueholder;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * QCResult represents a single quality control measurement from an instrument.
 */
@Entity
@Table(name = "qc_result")
public class QCResult extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @NotNull
    @Column(name = "control_lot_id", nullable = false, length = 36)
    private String controlLotId;

    @NotNull
    @Column(name = "test_id", nullable = false)
    private Integer testId;

    @NotNull
    @Column(name = "instrument_id", nullable = false)
    private Integer instrumentId;

    @NotNull
    @Column(name = "result_value", nullable = false, precision = 15, scale = 5)
    private BigDecimal resultValue;

    @NotNull
    @Column(name = "unit_of_measure", nullable = false, length = 50)
    private String unitOfMeasure;

    @Column(name = "z_score", precision = 10, scale = 4)
    private BigDecimal zScore;

    @NotNull
    @Column(name = "run_date_time", nullable = false)
    private Timestamp runDateTime;

    @Column(name = "technician_id")
    private Integer technicianId;

    @NotNull
    @Column(name = "result_status", nullable = false, length = 50)
    private String resultStatus = "PENDING";

    @Column(name = "non_conformity_flag")
    private Boolean nonConformityFlag = false;

    @Column(name = "external_notes", columnDefinition = "TEXT")
    private String externalNotes;

    public QCResult() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getControlLotId() {
        return controlLotId;
    }

    public void setControlLotId(String controlLotId) {
        this.controlLotId = controlLotId;
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

    public BigDecimal getResultValue() {
        return resultValue;
    }

    public void setResultValue(BigDecimal resultValue) {
        this.resultValue = resultValue;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getZScore() {
        return zScore;
    }

    public void setZScore(BigDecimal zScore) {
        this.zScore = zScore;
    }

    public Timestamp getRunDateTime() {
        return runDateTime;
    }

    public void setRunDateTime(Timestamp runDateTime) {
        this.runDateTime = runDateTime;
    }

    public Integer getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Integer technicianId) {
        this.technicianId = technicianId;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public Boolean getNonConformityFlag() {
        return nonConformityFlag;
    }

    public void setNonConformityFlag(Boolean nonConformityFlag) {
        this.nonConformityFlag = nonConformityFlag;
    }

    public String getExternalNotes() {
        return externalNotes;
    }

    public void setExternalNotes(String externalNotes) {
        this.externalNotes = externalNotes;
    }
}
