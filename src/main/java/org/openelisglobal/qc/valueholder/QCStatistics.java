package org.openelisglobal.qc.valueholder;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * QCStatistics represents calculated statistical parameters for a control lot.
 * Used for caching mean and standard deviation to avoid recalculation.
 */
@Entity
@Table(name = "qc_statistics")
public class QCStatistics extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @NotNull
    @Column(name = "control_lot_id", nullable = false, length = 36)
    private String controlLotId;

    @NotNull
    @Column(name = "calculation_date", nullable = false)
    private Timestamp calculationDate;

    @NotNull
    @Column(name = "mean", nullable = false, precision = 15, scale = 5)
    private BigDecimal mean;

    @NotNull
    @Column(name = "standard_deviation", nullable = false, precision = 15, scale = 5)
    private BigDecimal standardDeviation;

    @NotNull
    @Column(name = "num_values", nullable = false)
    private Integer numValues;

    @NotNull
    @Column(name = "calculation_method", nullable = false, length = 50)
    private String calculationMethod;

    @NotNull
    @Column(name = "validity_start", nullable = false)
    private Timestamp validityStart;

    @Column(name = "validity_end")
    private Timestamp validityEnd;

    public QCStatistics() {
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

    public Timestamp getCalculationDate() {
        return calculationDate;
    }

    public void setCalculationDate(Timestamp calculationDate) {
        this.calculationDate = calculationDate;
    }

    public BigDecimal getMean() {
        return mean;
    }

    public void setMean(BigDecimal mean) {
        this.mean = mean;
    }

    public BigDecimal getStandardDeviation() {
        return standardDeviation;
    }

    public void setStandardDeviation(BigDecimal standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    public Integer getNumValues() {
        return numValues;
    }

    public void setNumValues(Integer numValues) {
        this.numValues = numValues;
    }

    public String getCalculationMethod() {
        return calculationMethod;
    }

    public void setCalculationMethod(String calculationMethod) {
        this.calculationMethod = calculationMethod;
    }

    public Timestamp getValidityStart() {
        return validityStart;
    }

    public void setValidityStart(Timestamp validityStart) {
        this.validityStart = validityStart;
    }

    public Timestamp getValidityEnd() {
        return validityEnd;
    }

    public void setValidityEnd(Timestamp validityEnd) {
        this.validityEnd = validityEnd;
    }
}
