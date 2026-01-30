package org.openelisglobal.tb.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.tb.valueholder.TbEnums.QcDestination;
import org.openelisglobal.tb.valueholder.TbEnums.QcResult;
import org.openelisglobal.tb.valueholder.TbEnums.RejectionReason;

/**
 * TB-specific quality check results for raw samples. Tracks leak, temperature,
 * packaging, labeling, volume, and request matching checks.
 */
@Entity
@Table(name = "tb_quality_check")
public class TbQualityCheck extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tb_quality_check_generator")
    @SequenceGenerator(name = "tb_quality_check_generator", sequenceName = "tb_quality_check_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_item_id", nullable = false)
    private SampleItem sampleItem;

    @Column(name = "qc_date", nullable = false)
    private Timestamp qcDate;

    // Individual QC check fields (true = pass, false = fail, null = not checked)
    @Column(name = "leak_check")
    private Boolean leakCheck;

    @Column(name = "temperature_check")
    private Boolean temperatureCheck;

    @Column(name = "packaging_check")
    private Boolean packagingCheck;

    @Column(name = "labeling_check")
    private Boolean labelingCheck;

    @Column(name = "volume_check")
    private Boolean volumeCheck;

    @Column(name = "request_match_check")
    private Boolean requestMatchCheck;

    // Overall result and rejection handling
    @Enumerated(EnumType.STRING)
    @Column(name = "overall_result", nullable = false, length = 50)
    private QcResult overallResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason", length = 100)
    private RejectionReason rejectionReason;

    @Column(name = "rejection_remarks", columnDefinition = "TEXT")
    private String rejectionRemarks;

    @Enumerated(EnumType.STRING)
    @Column(name = "destination", length = 50)
    private QcDestination destination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by")
    private SystemUser checkedBy;

    // Getters and setters
    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public SampleItem getSampleItem() {
        return sampleItem;
    }

    public void setSampleItem(SampleItem sampleItem) {
        this.sampleItem = sampleItem;
    }

    public String getSampleItemId() {
        return sampleItem != null ? sampleItem.getId() : null;
    }

    public Timestamp getQcDate() {
        return qcDate;
    }

    public void setQcDate(Timestamp qcDate) {
        this.qcDate = qcDate;
    }

    public Boolean getLeakCheck() {
        return leakCheck;
    }

    public void setLeakCheck(Boolean leakCheck) {
        this.leakCheck = leakCheck;
    }

    public Boolean getTemperatureCheck() {
        return temperatureCheck;
    }

    public void setTemperatureCheck(Boolean temperatureCheck) {
        this.temperatureCheck = temperatureCheck;
    }

    public Boolean getPackagingCheck() {
        return packagingCheck;
    }

    public void setPackagingCheck(Boolean packagingCheck) {
        this.packagingCheck = packagingCheck;
    }

    public Boolean getLabelingCheck() {
        return labelingCheck;
    }

    public void setLabelingCheck(Boolean labelingCheck) {
        this.labelingCheck = labelingCheck;
    }

    public Boolean getVolumeCheck() {
        return volumeCheck;
    }

    public void setVolumeCheck(Boolean volumeCheck) {
        this.volumeCheck = volumeCheck;
    }

    public Boolean getRequestMatchCheck() {
        return requestMatchCheck;
    }

    public void setRequestMatchCheck(Boolean requestMatchCheck) {
        this.requestMatchCheck = requestMatchCheck;
    }

    public QcResult getOverallResult() {
        return overallResult;
    }

    public void setOverallResult(QcResult overallResult) {
        this.overallResult = overallResult;
    }

    public RejectionReason getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(RejectionReason rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRejectionRemarks() {
        return rejectionRemarks;
    }

    public void setRejectionRemarks(String rejectionRemarks) {
        this.rejectionRemarks = rejectionRemarks;
    }

    public QcDestination getDestination() {
        return destination;
    }

    public void setDestination(QcDestination destination) {
        this.destination = destination;
    }

    public SystemUser getCheckedBy() {
        return checkedBy;
    }

    public void setCheckedBy(SystemUser checkedBy) {
        this.checkedBy = checkedBy;
    }

    /**
     * Returns true if the QC result indicates a failure that was proceeded with.
     */
    public boolean hasQcFailure() {
        return overallResult == QcResult.FAIL_PROCEED;
    }

    /**
     * Returns true if all individual checks passed.
     */
    public boolean allChecksPassed() {
        return Boolean.TRUE.equals(leakCheck) && Boolean.TRUE.equals(temperatureCheck)
                && Boolean.TRUE.equals(packagingCheck) && Boolean.TRUE.equals(labelingCheck)
                && Boolean.TRUE.equals(volumeCheck) && Boolean.TRUE.equals(requestMatchCheck);
    }
}
