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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Provider-side follow-up with a participating lab after poor performance
 * (FR-V2.1-13). One open register row per cycle per organisation.
 *
 * <p>
 * The result summary is stored as a JSON snapshot rather than recomputed, so
 * the register still reads correctly after later cycles change the underlying
 * rows.
 */
@Getter
@Setter
@Entity
@Table(name = "eqa_participant_followup", uniqueConstraints = @UniqueConstraint(name = "uq_eqa_participant_followup_cycle_org", columnNames = {
        "cycle_id", "participant_org_id" }))
public class EQAParticipantFollowup extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_participant_followup_generator")
    @SequenceGenerator(name = "eqa_participant_followup_generator", sequenceName = "eqa_participant_followup_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private EQAProgram scheme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private EQACycle cycle;

    @Column(name = "participant_org_id", nullable = false)
    private Long participantOrgId;

    @Column(name = "participant_result_summary_json", columnDefinition = "TEXT")
    private String participantResultSummaryJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "followup_status", nullable = false, length = 30)
    private EQAFollowupStatus followupStatus = EQAFollowupStatus.NOTIFIED;

    @Column(name = "notified_at")
    private Timestamp notifiedAt;

    @Column(name = "response_received_at")
    private Timestamp responseReceivedAt;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "persistent_failure_flag", nullable = false)
    private Boolean persistentFailureFlag = false;

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
}
