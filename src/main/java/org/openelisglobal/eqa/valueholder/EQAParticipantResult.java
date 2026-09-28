package org.openelisglobal.eqa.valueholder;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * A lab's per-analyte result within a round — the EQA-side view V1 lacks.
 * resultValue is a String on purpose: the six ePT-validated domains include
 * qualitative ("Reactive"), categorical ("Recent") and semi-quantitative ("3+")
 * results.
 *
 * <p>
 * Reference columns are raw Longs in the module's SampleEQA.eqaEnrollmentId
 * style. (An earlier revision claimed EQALabProgramEnrollment is unmapped in
 * production; that is wrong — the production unit scans its classpath root and
 * maps it, verified empirically. The raw-Long style stands on the module
 * precedent alone.)
 */
@Getter
@Setter
@Entity
// Uniqueness is two partial indexes (qa/027, qa/047): one row per aliquot for
// in-house panels, and one per analyte per provider sample for external PT. A
// table-level constraint here would have Hibernate recreate the old single rule.
@Table(name = "eqa_participant_result")
public class EQAParticipantResult extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_participant_result_generator")
    @SequenceGenerator(name = "eqa_participant_result_generator", sequenceName = "eqa_participant_result_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private EQACycle cycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private EQARound round;

    @Column(name = "lab_enrollment_id", nullable = false)
    private Long labEnrollmentId;

    @Column(name = "analyte_id", nullable = false)
    private Long analyteId;

    /** FK to the standard analysis row; NULL for the MANUAL channel. */
    @Column(name = "analysis_id")
    private Long analysisId;

    /**
     * The exact aliquot this result answers, for in-house panels. Null for external
     * PT, where the analyte alone identifies the result — the partial unique
     * indexes in qa/027 enforce one rule per case.
     */
    @Column(name = "panel_sample_id")
    private Long panelSampleId;

    /**
     * External PT: the provider's code for the panel sample this result answers, as
     * the laboratory entered it with the order. Two samples of one analyte are two
     * results, told apart by this code on the export bundle and the scores the
     * provider returns. Null where the order named no provider sample.
     */
    @Column(name = "provider_sample_code", length = 100)
    private String providerSampleCode;

    /**
     * External PT: the target the provider scored this result against, as its
     * scores reported it. The provider keeps the panel, so this is the only local
     * record of it.
     */
    @Column(name = "provider_target", length = 255)
    private String providerTarget;

    @Column(name = "result_value", length = 255)
    private String resultValue;

    @Column(name = "result_unit", length = 50)
    private String resultUnit;

    @Column(name = "assigned_analyst_id")
    private Long assignedAnalystId;

    @Column(name = "entered_by")
    private Long enteredBy;

    @Column(name = "entered_at")
    private Timestamp enteredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_status", nullable = false, length = 20)
    private EQASubmissionStatus submissionStatus = EQASubmissionStatus.DRAFT;

    @Column(name = "submitted_at")
    private Timestamp submittedAt;

    /** The scoring verdict; null until the result is scored. */
    @Enumerated(EnumType.STRING)
    @Column(name = "performance_status", length = 20)
    private EQAPerformanceStatus performanceStatus;

    /** Null for in-house, which has no consensus SD by construction. */
    @Column(name = "z_score", precision = 10, scale = 5)
    private BigDecimal zScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_channel", length = 10)
    private EQASubmissionChannel submissionChannel;

    @Column(name = "manual_submission_reference", length = 255)
    private String manualSubmissionReference;

    /** Link to the V1 eqa_result scoring row once the provider scores. */
    @Column(name = "eqa_result_id")
    private Long eqaResultId;

    @Column(name = "score_received_at")
    private Timestamp scoreReceivedAt;

    @Column(name = "sys_user_id", nullable = false)
    private String sysUserId;

    @Override
    public String getSysUserId() {
        return sysUserId;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }

    @PrePersist
    public void prePersist() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }
}
