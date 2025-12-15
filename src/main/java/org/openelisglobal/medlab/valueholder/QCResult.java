/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.medlab.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * QCResult entity for tracking quality control results for Levey-Jennings
 * charting.
 *
 * <p>
 * Supports dual-level QC (Normal and Pathologic) as required by CLIA.
 * Implements Westgard rule validation for detecting systematic and random
 * errors:
 * <ul>
 * <li>1:2s - Warning rule (one result exceeds 2 SD)</li>
 * <li>1:3s - Rejection rule (one result exceeds 3 SD)</li>
 * <li>2:2s - Rejection rule (two consecutive results exceed 2 SD same
 * side)</li>
 * <li>R:4s - Rejection rule (range of two results exceeds 4 SD)</li>
 * <li>4:1s - Rejection rule (four consecutive results exceed 1 SD same
 * side)</li>
 * <li>10x - Rejection rule (ten consecutive results on same side of mean)</li>
 * </ul>
 */
@Entity
@Table(name = "qc_result")
public class QCResult extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    public enum QCLevel {
        NORMAL, PATHOLOGIC
    }

    public enum PassFail {
        PASS, FAIL
    }

    public enum WestgardRule {
        NONE, ONE_2S, // 1:2s - Warning
        ONE_3S, // 1:3s - Rejection
        TWO_2S, // 2:2s - Rejection
        R_4S, // R:4s - Rejection
        FOUR_1S, // 4:1s - Rejection
        TEN_X // 10x - Rejection
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "qc_result_seq")
    @SequenceGenerator(name = "qc_result_seq", sequenceName = "qc_result_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "test_id", nullable = false)
    private Integer testId;

    @Column(name = "analyzer_id")
    private Integer analyzerId;

    @Column(name = "qc_level", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private QCLevel qcLevel;

    @Column(name = "lot_number", nullable = false, length = 50)
    private String lotNumber;

    @Column(name = "expiry_date")
    private Date expiryDate;

    @Column(name = "target_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal targetValue;

    @Column(name = "sd_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal sdValue;

    @Column(name = "acceptable_range_low", nullable = false, precision = 10, scale = 4)
    private BigDecimal acceptableRangeLow;

    @Column(name = "acceptable_range_high", nullable = false, precision = 10, scale = 4)
    private BigDecimal acceptableRangeHigh;

    @Column(name = "result_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal resultValue;

    @Column(name = "result_date", nullable = false)
    private Date resultDate;

    @Column(name = "result_time", nullable = false)
    private Time resultTime;

    @Column(name = "performed_by", nullable = false)
    private Integer performedBy;

    @Column(name = "pass_fail", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private PassFail passFail;

    @Column(name = "westgard_rule_violated", length = 20)
    @Enumerated(EnumType.STRING)
    private WestgardRule westgardRuleViolated;

    @Column(name = "corrective_action", length = 255)
    private String correctiveAction;

    @Column(name = "action_taken_by")
    private Integer actionTakenBy;

    @Column(name = "action_date")
    private Timestamp actionDate;

    @Column(name = "is_calibration")
    private Boolean isCalibration = false;

    @Column(name = "fhir_uuid", unique = true)
    private UUID fhirUuid;

    public QCResult() {
        super();
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTestId() {
        return testId;
    }

    public void setTestId(Integer testId) {
        this.testId = testId;
    }

    public Integer getAnalyzerId() {
        return analyzerId;
    }

    public void setAnalyzerId(Integer analyzerId) {
        this.analyzerId = analyzerId;
    }

    public QCLevel getQcLevel() {
        return qcLevel;
    }

    public void setQcLevel(QCLevel qcLevel) {
        this.qcLevel = qcLevel;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(BigDecimal targetValue) {
        this.targetValue = targetValue;
    }

    public BigDecimal getSdValue() {
        return sdValue;
    }

    public void setSdValue(BigDecimal sdValue) {
        this.sdValue = sdValue;
    }

    public BigDecimal getAcceptableRangeLow() {
        return acceptableRangeLow;
    }

    public void setAcceptableRangeLow(BigDecimal acceptableRangeLow) {
        this.acceptableRangeLow = acceptableRangeLow;
    }

    public BigDecimal getAcceptableRangeHigh() {
        return acceptableRangeHigh;
    }

    public void setAcceptableRangeHigh(BigDecimal acceptableRangeHigh) {
        this.acceptableRangeHigh = acceptableRangeHigh;
    }

    public BigDecimal getResultValue() {
        return resultValue;
    }

    public void setResultValue(BigDecimal resultValue) {
        this.resultValue = resultValue;
    }

    public Date getResultDate() {
        return resultDate;
    }

    public void setResultDate(Date resultDate) {
        this.resultDate = resultDate;
    }

    public Time getResultTime() {
        return resultTime;
    }

    public void setResultTime(Time resultTime) {
        this.resultTime = resultTime;
    }

    public Integer getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(Integer performedBy) {
        this.performedBy = performedBy;
    }

    public PassFail getPassFail() {
        return passFail;
    }

    public void setPassFail(PassFail passFail) {
        this.passFail = passFail;
    }

    public WestgardRule getWestgardRuleViolated() {
        return westgardRuleViolated;
    }

    public void setWestgardRuleViolated(WestgardRule westgardRuleViolated) {
        this.westgardRuleViolated = westgardRuleViolated;
    }

    public String getCorrectiveAction() {
        return correctiveAction;
    }

    public void setCorrectiveAction(String correctiveAction) {
        this.correctiveAction = correctiveAction;
    }

    public Integer getActionTakenBy() {
        return actionTakenBy;
    }

    public void setActionTakenBy(Integer actionTakenBy) {
        this.actionTakenBy = actionTakenBy;
    }

    public Timestamp getActionDate() {
        return actionDate;
    }

    public void setActionDate(Timestamp actionDate) {
        this.actionDate = actionDate;
    }

    public Boolean getIsCalibration() {
        return isCalibration;
    }

    public void setIsCalibration(Boolean isCalibration) {
        this.isCalibration = isCalibration;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    /**
     * Calculates the Z-score (number of standard deviations from mean).
     *
     * @return Z-score value
     */
    public BigDecimal calculateZScore() {
        if (sdValue == null || sdValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return resultValue.subtract(targetValue).divide(sdValue, 4, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Determines if result is within acceptable range.
     *
     * @return true if within range, false otherwise
     */
    public boolean isWithinAcceptableRange() {
        return resultValue.compareTo(acceptableRangeLow) >= 0 && resultValue.compareTo(acceptableRangeHigh) <= 0;
    }

    /**
     * Determines if result exceeds 2 SD (warning threshold).
     *
     * @return true if exceeds 2 SD
     */
    public boolean exceedsTwoSd() {
        BigDecimal zScore = calculateZScore().abs();
        return zScore.compareTo(new BigDecimal("2.0")) > 0;
    }

    /**
     * Determines if result exceeds 3 SD (rejection threshold).
     *
     * @return true if exceeds 3 SD
     */
    public boolean exceedsThreeSd() {
        BigDecimal zScore = calculateZScore().abs();
        return zScore.compareTo(new BigDecimal("3.0")) > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        QCResult that = (QCResult) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
