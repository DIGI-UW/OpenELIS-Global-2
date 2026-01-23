package org.openelisglobal.biorepository.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Entity representing a QC inspection record for a biorepository sample. Used
 * to verify physical presence, label integrity, container condition, and
 * correct storage position of samples with workflowStatus = STORED.
 *
 * Part of the biorepository quality control and integrity assurance workflow.
 */
@Entity
@Table(name = "biorepository_qc_inspection", schema = "clinlims")
public class BiorepositoryQCInspection extends BaseObject<Integer> {

    /**
     * Overall QC inspection result.
     */
    public enum QCResult {
        VERIFIED("Verified - All checks passed"), DISCREPANCY_FOUND("Discrepancy Found");

        private final String displayValue;

        QCResult(String displayValue) {
            this.displayValue = displayValue;
        }

        public String getDisplayValue() {
            return displayValue;
        }
    }

    /**
     * Types of discrepancies that can be found during QC inspection.
     */
    public enum DiscrepancyType {
        MISSING_SAMPLE("Missing Sample"), DAMAGED_LABEL("Damaged/Illegible Label"),
        MISPLACED_ITEM("Misplaced Item (wrong position)"), CONTAINER_DAMAGE("Container Damage"),
        VOLUME_DISCREPANCY("Volume Discrepancy"), OTHER("Other");

        private final String displayValue;

        DiscrepancyType(String displayValue) {
            this.displayValue = displayValue;
        }

        public String getDisplayValue() {
            return displayValue;
        }

        /**
         * Parse string to enum, returning null if invalid.
         */
        public static DiscrepancyType fromString(String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            for (DiscrepancyType type : values()) {
                if (type.name().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return null;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "qc_inspection_generator")
    @SequenceGenerator(name = "qc_inspection_generator", sequenceName = "biorepository_qc_inspection_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @NotNull(message = "BioSample is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "biosample_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private BioSample bioSample;

    // ========================================
    // QC Checklist Items
    // ========================================

    @Column(name = "sample_present", nullable = false)
    private boolean samplePresent = false;

    @Column(name = "label_integrity", nullable = false)
    private boolean labelIntegrity = false;

    @Column(name = "container_integrity", nullable = false)
    private boolean containerIntegrity = false;

    @Column(name = "volume_appearance_acceptable", nullable = false)
    private boolean volumeAppearanceAcceptable = false;

    @Column(name = "correct_position", nullable = false)
    private boolean correctPosition = false;

    // ========================================
    // QC Result
    // ========================================

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "qc_result", nullable = false, length = 30)
    private QCResult qcResult = QCResult.VERIFIED;

    // ========================================
    // Discrepancy Details (if QC failed)
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(name = "discrepancy_type", length = 30)
    private DiscrepancyType discrepancyType;

    @Column(name = "corrective_action", columnDefinition = "TEXT")
    private String correctiveAction;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    // ========================================
    // Inspection Metadata
    // ========================================

    @NotNull(message = "Inspector name is required")
    @Column(name = "inspector_name", nullable = false, length = 100)
    private String inspectorName;

    @NotNull(message = "Inspection date is required")
    @Column(name = "inspection_date", nullable = false)
    private Timestamp inspectionDate;

    @Column(name = "sys_user_id", nullable = false, length = 36)
    private String sysUserId;

    // Default constructor required by JPA
    public BiorepositoryQCInspection() {
    }

    /**
     * Constructor with required fields.
     *
     * @param bioSample      the biosample being inspected
     * @param inspectorName  name of the person performing the inspection
     * @param inspectionDate date/time of the inspection
     */
    public BiorepositoryQCInspection(BioSample bioSample, String inspectorName, Timestamp inspectionDate) {
        this.bioSample = bioSample;
        this.inspectorName = inspectorName;
        this.inspectionDate = inspectionDate;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String getSysUserId() {
        return sysUserId;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }

    public BioSample getBioSample() {
        return bioSample;
    }

    public void setBioSample(BioSample bioSample) {
        this.bioSample = bioSample;
    }

    public boolean isSamplePresent() {
        return samplePresent;
    }

    public void setSamplePresent(boolean samplePresent) {
        this.samplePresent = samplePresent;
    }

    public boolean isLabelIntegrity() {
        return labelIntegrity;
    }

    public void setLabelIntegrity(boolean labelIntegrity) {
        this.labelIntegrity = labelIntegrity;
    }

    public boolean isContainerIntegrity() {
        return containerIntegrity;
    }

    public void setContainerIntegrity(boolean containerIntegrity) {
        this.containerIntegrity = containerIntegrity;
    }

    public boolean isVolumeAppearanceAcceptable() {
        return volumeAppearanceAcceptable;
    }

    public void setVolumeAppearanceAcceptable(boolean volumeAppearanceAcceptable) {
        this.volumeAppearanceAcceptable = volumeAppearanceAcceptable;
    }

    public boolean isCorrectPosition() {
        return correctPosition;
    }

    public void setCorrectPosition(boolean correctPosition) {
        this.correctPosition = correctPosition;
    }

    public QCResult getQcResult() {
        return qcResult;
    }

    public void setQcResult(QCResult qcResult) {
        this.qcResult = qcResult;
    }

    public DiscrepancyType getDiscrepancyType() {
        return discrepancyType;
    }

    public void setDiscrepancyType(DiscrepancyType discrepancyType) {
        this.discrepancyType = discrepancyType;
    }

    public String getCorrectiveAction() {
        return correctiveAction;
    }

    public void setCorrectiveAction(String correctiveAction) {
        this.correctiveAction = correctiveAction;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getInspectorName() {
        return inspectorName;
    }

    public void setInspectorName(String inspectorName) {
        this.inspectorName = inspectorName;
    }

    public Timestamp getInspectionDate() {
        return inspectionDate;
    }

    public void setInspectionDate(Timestamp inspectionDate) {
        this.inspectionDate = inspectionDate;
    }

    /**
     * Checks if all QC checklist items passed.
     *
     * @return true if all items are checked
     */
    public boolean isAllChecksPassed() {
        return samplePresent && labelIntegrity && containerIntegrity && volumeAppearanceAcceptable && correctPosition;
    }

    /**
     * Auto-calculates and sets QC result based on checklist completion. Call this
     * after updating checklist items.
     */
    public void updateQcResult() {
        this.qcResult = isAllChecksPassed() ? QCResult.VERIFIED : QCResult.DISCREPANCY_FOUND;
    }

    /**
     * Counts how many checklist items passed.
     *
     * @return number of checklist items that passed (0-5)
     */
    public int getPassedCheckCount() {
        int count = 0;
        if (samplePresent)
            count++;
        if (labelIntegrity)
            count++;
        if (containerIntegrity)
            count++;
        if (volumeAppearanceAcceptable)
            count++;
        if (correctPosition)
            count++;
        return count;
    }

    /**
     * Total number of checklist items.
     *
     * @return 5 (the number of QC checklist items)
     */
    public int getTotalCheckCount() {
        return 5;
    }
}
