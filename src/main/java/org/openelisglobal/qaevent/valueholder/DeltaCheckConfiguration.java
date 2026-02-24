package org.openelisglobal.qaevent.valueholder;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.test.valueholder.Test;

/**
 * Entity class for configuring delta check thresholds.
 * Delta checks compare new lab results with previous results for the same patient
 * and generate alerts when the change exceeds configured thresholds.
 */
@Entity
@Table(name = "delta_check_configuration")
public class DeltaCheckConfiguration extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", referencedColumnName = "id")
    private Test test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyte_id", referencedColumnName = "id")
    private Analyte analyte;

    @Basic
    @Column(name = "threshold_percent", precision = 6, scale = 2)
    private BigDecimal thresholdPercent;

    @Basic
    @Column(name = "active")
    private Boolean active;

    @Basic
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Basic
    @Column(name = "created_date")
    private Timestamp createdDate;

    @Basic
    @Column(name = "modified_by", length = 100)
    private String modifiedBy;

    @Basic
    @Column(name = "modified_date")
    private Timestamp modifiedDate;

    public DeltaCheckConfiguration() {
        super();
        this.active = true;
    }

    public DeltaCheckConfiguration(Test test, BigDecimal thresholdPercent, String createdBy) {
        this();
        this.test = test;
        this.thresholdPercent = thresholdPercent;
        this.createdBy = createdBy;
        this.createdDate = new Timestamp(System.currentTimeMillis());
    }

    public DeltaCheckConfiguration(Test test, Analyte analyte, BigDecimal thresholdPercent, String createdBy) {
        this(test, thresholdPercent, createdBy);
        this.analyte = analyte;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public Analyte getAnalyte() {
        return analyte;
    }

    public void setAnalyte(Analyte analyte) {
        this.analyte = analyte;
    }

    public BigDecimal getThresholdPercent() {
        return thresholdPercent;
    }

    public void setThresholdPercent(BigDecimal thresholdPercent) {
        this.thresholdPercent = thresholdPercent;
    }

    /**
     * Convenience method to get threshold as double
     */
    public double getThresholdPercentAsDouble() {
        return thresholdPercent != null ? thresholdPercent.doubleValue() : 0.0;
    }

    /**
     * Convenience method to set threshold from double
     */
    public void setThresholdPercentAsDouble(double thresholdPercent) {
        this.thresholdPercent = BigDecimal.valueOf(thresholdPercent);
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    /**
     * Convenience method for boolean operations
     */
    public boolean isActive() {
        return active != null && active;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Timestamp getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Timestamp modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    /**
     * Update modification tracking fields
     */
    public void updateModificationInfo(String modifiedBy) {
        this.modifiedBy = modifiedBy;
        this.modifiedDate = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Check if this configuration is more specific than another.
     * Analyte-specific configurations are more specific than test-level configurations.
     */
    public boolean isMoreSpecificThan(DeltaCheckConfiguration other) {
        if (other == null) {
            return true;
        }

        // If this has an analyte and the other doesn't, this is more specific
        if (this.analyte != null && other.analyte == null) {
            return true;
        }

        // If the other has an analyte and this doesn't, the other is more specific
        if (this.analyte == null && other.analyte != null) {
            return false;
        }

        // Both are at the same level of specificity
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeltaCheckConfiguration that = (DeltaCheckConfiguration) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DeltaCheckConfiguration{" +
                "id=" + id +
                ", testId=" + (test != null ? test.getId() : null) +
                ", analyteId=" + (analyte != null ? analyte.getId() : null) +
                ", thresholdPercent=" + thresholdPercent +
                ", active=" + active +
                ", createdBy='" + createdBy + '\'' +
                ", createdDate=" + createdDate +
                '}';
    }
}