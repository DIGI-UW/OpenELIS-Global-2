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
import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * QualityCheck entity for tracking quality control checks performed during
 * sample reception.
 *
 * <p>
 * Supports sample-type-specific QC criteria including:
 *
 * <ul>
 * <li>Chemistry: hemolysis, lipemia, icterus, volume, delay
 * <li>Hematology: clotting, anticoagulant type, delay
 * <li>Stool: delay (&gt;30 min), container, contamination
 * <li>Urine: delay, leakage, container
 * <li>Microbiology: delay, contamination, container
 * </ul>
 */
@Entity
@Table(name = "quality_check")
public class QualityCheck extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    public enum OverallStatus {
        ACCEPTED, REJECTED
    }

    public enum CorrectiveAction {
        RECOLLECTION, RETURN_TO_SUBMITTER
    }

    public enum AnticoagulantType {
        EDTA, CITRATE, HEPARIN, FLUORIDE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "quality_check_seq")
    @SequenceGenerator(name = "quality_check_seq", sequenceName = "quality_check_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "sample_item_id", nullable = false)
    private Integer sampleItemId;

    @Column(name = "check_date", nullable = false)
    private Timestamp checkDate;

    @Column(name = "checked_by", nullable = false)
    private Integer checkedBy;

    @Column(name = "sample_type", nullable = false, length = 50)
    private String sampleType;

    @Column(name = "delay_minutes")
    private Integer delayMinutes;

    @Column(name = "delay_passed")
    private Boolean delayPassed = true;

    @Column(name = "volume_ml", precision = 5, scale = 2)
    private BigDecimal volumeMl;

    @Column(name = "volume_passed")
    private Boolean volumePassed = true;

    @Column(name = "hemolysis")
    private Boolean hemolysis = false;

    @Column(name = "lipemia")
    private Boolean lipemia = false;

    @Column(name = "icterus")
    private Boolean icterus = false;

    @Column(name = "clotting")
    private Boolean clotting = false;

    @Column(name = "anticoagulant_type", length = 20)
    @Enumerated(EnumType.STRING)
    private AnticoagulantType anticoagulantType;

    @Column(name = "contamination")
    private Boolean contamination = false;

    @Column(name = "container_proper")
    private Boolean containerProper = true;

    @Column(name = "leakage")
    private Boolean leakage = false;

    @Column(name = "overall_status", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private OverallStatus overallStatus;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "corrective_action", length = 50)
    @Enumerated(EnumType.STRING)
    private CorrectiveAction correctiveAction;

    @Column(name = "fhir_uuid", unique = true)
    private UUID fhirUuid;

    public QualityCheck() {
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

    public Integer getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(Integer sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public Timestamp getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(Timestamp checkDate) {
        this.checkDate = checkDate;
    }

    public Integer getCheckedBy() {
        return checkedBy;
    }

    public void setCheckedBy(Integer checkedBy) {
        this.checkedBy = checkedBy;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public Integer getDelayMinutes() {
        return delayMinutes;
    }

    public void setDelayMinutes(Integer delayMinutes) {
        this.delayMinutes = delayMinutes;
    }

    public Boolean getDelayPassed() {
        return delayPassed;
    }

    public void setDelayPassed(Boolean delayPassed) {
        this.delayPassed = delayPassed;
    }

    public BigDecimal getVolumeMl() {
        return volumeMl;
    }

    public void setVolumeMl(BigDecimal volumeMl) {
        this.volumeMl = volumeMl;
    }

    public Boolean getVolumePassed() {
        return volumePassed;
    }

    public void setVolumePassed(Boolean volumePassed) {
        this.volumePassed = volumePassed;
    }

    public Boolean getHemolysis() {
        return hemolysis;
    }

    public void setHemolysis(Boolean hemolysis) {
        this.hemolysis = hemolysis;
    }

    public Boolean getLipemia() {
        return lipemia;
    }

    public void setLipemia(Boolean lipemia) {
        this.lipemia = lipemia;
    }

    public Boolean getIcterus() {
        return icterus;
    }

    public void setIcterus(Boolean icterus) {
        this.icterus = icterus;
    }

    public Boolean getClotting() {
        return clotting;
    }

    public void setClotting(Boolean clotting) {
        this.clotting = clotting;
    }

    public AnticoagulantType getAnticoagulantType() {
        return anticoagulantType;
    }

    public void setAnticoagulantType(AnticoagulantType anticoagulantType) {
        this.anticoagulantType = anticoagulantType;
    }

    public Boolean getContamination() {
        return contamination;
    }

    public void setContamination(Boolean contamination) {
        this.contamination = contamination;
    }

    public Boolean getContainerProper() {
        return containerProper;
    }

    public void setContainerProper(Boolean containerProper) {
        this.containerProper = containerProper;
    }

    public Boolean getLeakage() {
        return leakage;
    }

    public void setLeakage(Boolean leakage) {
        this.leakage = leakage;
    }

    public OverallStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(OverallStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public CorrectiveAction getCorrectiveAction() {
        return correctiveAction;
    }

    public void setCorrectiveAction(CorrectiveAction correctiveAction) {
        this.correctiveAction = correctiveAction;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    /**
     * Determines if all quality checks passed.
     *
     * @return true if all checks passed, false otherwise
     */
    public boolean allChecksPassed() {
        return Boolean.TRUE.equals(delayPassed) && Boolean.TRUE.equals(volumePassed) && Boolean.FALSE.equals(hemolysis)
                && Boolean.FALSE.equals(lipemia) && Boolean.FALSE.equals(icterus) && Boolean.FALSE.equals(clotting)
                && Boolean.FALSE.equals(contamination) && Boolean.TRUE.equals(containerProper)
                && Boolean.FALSE.equals(leakage);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        QualityCheck that = (QualityCheck) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
