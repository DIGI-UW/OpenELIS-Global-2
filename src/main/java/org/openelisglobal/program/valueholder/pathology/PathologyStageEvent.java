package org.openelisglobal.program.valueholder.pathology;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * One stage a pathology case entered: who moved it there and when, and who
 * moved it on and when (ISO 15189:2022 7.3.1(d)). A row is added on each move
 * and completed when the case leaves the stage; the only thing the bench
 * records on it is the processor run, once, at Processing. The case itself
 * holds one status, so a second pass through a stage would overwrite per-stage
 * columns; one row per entry keeps every pass.
 */
@Entity
@Table(name = "pathology_stage_event")
public class PathologyStageEvent extends BaseObject<Integer> {

    /** Width of the reason column. */
    public static final int REASON_MAX_LENGTH = 255;

    /** Width of the run_reference column. */
    public static final int RUN_REFERENCE_MAX_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pathology_stage_event_generator")
    @SequenceGenerator(name = "pathology_stage_event_generator", sequenceName = "pathology_stage_event_seq", allocationSize = 1)
    private Integer id;

    /**
     * The case, held as a plain id so the case's own save never touches this row.
     */
    @Column(name = "pathology_sample_id", nullable = false)
    private Integer pathologySampleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", length = 32, nullable = false)
    private PathologyStatus stage;

    /** Server clock at the move onto the stage. */
    @Column(name = "entered_at", nullable = false)
    private Timestamp enteredAt;

    /** The signed-in user who moved the case onto the stage. */
    @ManyToOne
    @JoinColumn(name = "entered_by", referencedColumnName = "id", nullable = false)
    private SystemUser enteredBy;

    /** Server clock at the move off the stage, null while the case stands there. */
    @Column(name = "completed_at")
    private Timestamp completedAt;

    @ManyToOne
    @JoinColumn(name = "completed_by", referencedColumnName = "id")
    private SystemUser completedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 8, nullable = false)
    private StageDirection direction;

    /**
     * Why the case was sent back; required on a return, absent on a move forward.
     */
    @Column(name = "reason", length = REASON_MAX_LENGTH)
    private String reason;

    /**
     * The processor run as written on the instrument, until a run registry exists.
     */
    @Column(name = "run_reference", length = RUN_REFERENCE_MAX_LENGTH)
    private String runReference;

    @Column(name = "notes")
    private String notes;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPathologySampleId() {
        return pathologySampleId;
    }

    public void setPathologySampleId(Integer pathologySampleId) {
        this.pathologySampleId = pathologySampleId;
    }

    public PathologyStatus getStage() {
        return stage;
    }

    public void setStage(PathologyStatus stage) {
        this.stage = stage;
    }

    public Timestamp getEnteredAt() {
        return enteredAt;
    }

    public void setEnteredAt(Timestamp enteredAt) {
        this.enteredAt = enteredAt;
    }

    public SystemUser getEnteredBy() {
        return enteredBy;
    }

    public void setEnteredBy(SystemUser enteredBy) {
        this.enteredBy = enteredBy;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

    public SystemUser getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(SystemUser completedBy) {
        this.completedBy = completedBy;
    }

    public StageDirection getDirection() {
        return direction;
    }

    public void setDirection(StageDirection direction) {
        this.direction = direction;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRunReference() {
        return runReference;
    }

    public void setRunReference(String runReference) {
        this.runReference = runReference;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
