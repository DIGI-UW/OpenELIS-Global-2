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
        // Document-aligned core discrepancy classes
        SAMPLE_MISSING("Sample missing"),
        WRONG_SAMPLE_IN_POSITION("Wrong sample in position"),
        MISPLACED_SAMPLE_FOUND("Misplaced sample (found elsewhere)"),
        EMPTY_POSITION_REGISTERED("Empty position but registered as occupied"),
        LABELING_ERROR("Labeling error"),
        BOX_RACK_MISPLACEMENT("Box/rack misplacement"),
        // Legacy values retained for backward compatibility with existing records
        MISSING_SAMPLE("Missing Sample"),
        DAMAGED_LABEL("Damaged/Illegible Label"),
        MISPLACED_ITEM("Misplaced Item (wrong position)"),
        CONTAINER_DAMAGE("Container Damage"),
        VOLUME_DISCREPANCY("Volume Discrepancy"),
        OTHER("Other");

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
            String normalized = value.trim().toUpperCase();
            switch (normalized) {
            case "SAMPLE_MISSING":
            case "MISSING_SAMPLE":
                return SAMPLE_MISSING;
            case "WRONG_SAMPLE_IN_POSITION":
            case "WRONG_SAMPLE":
                return WRONG_SAMPLE_IN_POSITION;
            case "MISPLACED_SAMPLE_FOUND":
            case "MISPLACED_SAMPLE_FOUND_ELSEWHERE":
            case "MISPLACED_ITEM":
                return MISPLACED_SAMPLE_FOUND;
            case "EMPTY_POSITION_REGISTERED":
            case "EMPTY_POSITION_BUT_REGISTERED_AS_OCCUPIED":
                return EMPTY_POSITION_REGISTERED;
            case "LABELING_ERROR":
            case "DAMAGED_LABEL":
                return LABELING_ERROR;
            case "BOX_RACK_MISPLACEMENT":
            case "BOX_OR_RACK_MISPLACEMENT":
            case "CONTAINER_DAMAGE":
                return BOX_RACK_MISPLACEMENT;
            default:
                break;
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
    // Expected coordinate snapshot at inspection time
    // ========================================

    @Column(name = "expected_location_path", columnDefinition = "TEXT")
    private String expectedLocationPath;

    @Column(name = "expected_position_coordinate", length = 50)
    private String expectedPositionCoordinate;

    // ========================================
    // Inspection Metadata
    // ========================================

    @NotNull(message = "Inspector name is required")
    @Column(name = "inspector_name", nullable = false, length = 100)
    private String inspectorName;

    @NotNull(message = "Inspection date is required")
    @Column(name = "inspection_date", nullable = false)
    private Timestamp inspectionDate;

    @Column(name = "qc_batch_id", length = 80)
    private String qcBatchId;

    @Column(name = "expected_coordinate_snapshot", length = 512)
    private String expectedCoordinateSnapshot;

    @Column(name = "correction_action_type", length = 50)
    private String correctionActionType;

    @Column(name = "correction_old_coordinate", columnDefinition = "TEXT")
    private String correctionOldCoordinate;

    @Column(name = "correction_new_coordinate", columnDefinition = "TEXT")
    private String correctionNewCoordinate;

    @Column(name = "correction_reason", columnDefinition = "TEXT")
    private String correctionReason;

    @Column(name = "correction_by_user", length = 36)
    private String correctionByUser;

    @Column(name = "correction_timestamp")
    private Timestamp correctionTimestamp;

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

    public String getExpectedLocationPath() {
        return expectedLocationPath;
    }

    public void setExpectedLocationPath(String expectedLocationPath) {
        this.expectedLocationPath = expectedLocationPath;
    }

    public String getExpectedPositionCoordinate() {
        return expectedPositionCoordinate;
    }

    public void setExpectedPositionCoordinate(String expectedPositionCoordinate) {
        this.expectedPositionCoordinate = expectedPositionCoordinate;
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

    public String getQcBatchId() {
        return qcBatchId;
    }

    public void setQcBatchId(String qcBatchId) {
        this.qcBatchId = qcBatchId;
    }

    public String getExpectedCoordinateSnapshot() {
        return expectedCoordinateSnapshot;
    }

    public void setExpectedCoordinateSnapshot(String expectedCoordinateSnapshot) {
        this.expectedCoordinateSnapshot = expectedCoordinateSnapshot;
    }

    public String getCorrectionActionType() {
        return correctionActionType;
    }

    public void setCorrectionActionType(String correctionActionType) {
        this.correctionActionType = correctionActionType;
    }

    public String getCorrectionOldCoordinate() {
        return correctionOldCoordinate;
    }

    public void setCorrectionOldCoordinate(String correctionOldCoordinate) {
        this.correctionOldCoordinate = correctionOldCoordinate;
    }

    public String getCorrectionNewCoordinate() {
        return correctionNewCoordinate;
    }

    public void setCorrectionNewCoordinate(String correctionNewCoordinate) {
        this.correctionNewCoordinate = correctionNewCoordinate;
    }

    public String getCorrectionReason() {
        return correctionReason;
    }

    public void setCorrectionReason(String correctionReason) {
        this.correctionReason = correctionReason;
    }

    public String getCorrectionByUser() {
        return correctionByUser;
    }

    public void setCorrectionByUser(String correctionByUser) {
        this.correctionByUser = correctionByUser;
    }

    public Timestamp getCorrectionTimestamp() {
        return correctionTimestamp;
    }

    public void setCorrectionTimestamp(Timestamp correctionTimestamp) {
        this.correctionTimestamp = correctionTimestamp;
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
