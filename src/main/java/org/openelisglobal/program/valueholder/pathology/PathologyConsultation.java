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
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * A second opinion on a pathology case (FR-11): who asked which pathologist,
 * why, whether before treatment, and what came back. A row is never removed: a
 * request that is no longer wanted is withdrawn and kept, so the case shows
 * every opinion that was sought.
 */
@Entity
@Table(name = "pathology_consultation")
public class PathologyConsultation extends BaseObject<Integer> {

    /** The longest outcome note a pathologist may record. */
    public static final int NOTE_MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pathology_consultation_generator")
    @SequenceGenerator(name = "pathology_consultation_generator", sequenceName = "pathology_consultation_seq", allocationSize = 1)
    private Integer id;

    /**
     * The case, held as a plain id so the case's own save never touches this row.
     */
    @Column(name = "pathology_sample_id", nullable = false)
    private Integer pathologySampleId;

    /** The signed-in user who asked for the opinion. */
    @ManyToOne
    @JoinColumn(name = "requested_by", referencedColumnName = "id", nullable = false)
    private SystemUser requestedBy;

    /** The pathologist asked, the only user who records the outcome. */
    @ManyToOne
    @JoinColumn(name = "consulted_user_id", referencedColumnName = "id", nullable = false)
    private SystemUser consultedUser;

    /** Dictionary id of the reason picked, if one was. */
    @Column(name = "reason_dictionary_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String reasonDictionaryId;

    /** Whether the opinion is asked for before the patient is treated. */
    @Column(name = "pre_treatment", nullable = false)
    private boolean preTreatment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private ConsultationStatus status;

    /** What the consulted pathologist concluded; set only once answered. */
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 16)
    private ConsultationOutcome outcome;

    @Column(name = "outcome_note")
    private String outcomeNote;

    /** Server clock when the opinion was asked for. */
    @Column(name = "requested_at", nullable = false)
    private Timestamp requestedAt;

    /** Server clock when the outcome was recorded. */
    @Column(name = "answered_at")
    private Timestamp answeredAt;

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

    public SystemUser getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(SystemUser requestedBy) {
        this.requestedBy = requestedBy;
    }

    public SystemUser getConsultedUser() {
        return consultedUser;
    }

    public void setConsultedUser(SystemUser consultedUser) {
        this.consultedUser = consultedUser;
    }

    public String getReasonDictionaryId() {
        return reasonDictionaryId;
    }

    public void setReasonDictionaryId(String reasonDictionaryId) {
        this.reasonDictionaryId = reasonDictionaryId;
    }

    public boolean isPreTreatment() {
        return preTreatment;
    }

    public void setPreTreatment(boolean preTreatment) {
        this.preTreatment = preTreatment;
    }

    public ConsultationStatus getStatus() {
        return status;
    }

    public void setStatus(ConsultationStatus status) {
        this.status = status;
    }

    public ConsultationOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(ConsultationOutcome outcome) {
        this.outcome = outcome;
    }

    public String getOutcomeNote() {
        return outcomeNote;
    }

    public void setOutcomeNote(String outcomeNote) {
        this.outcomeNote = outcomeNote;
    }

    public Timestamp getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Timestamp requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Timestamp getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(Timestamp answeredAt) {
        this.answeredAt = answeredAt;
    }
}
