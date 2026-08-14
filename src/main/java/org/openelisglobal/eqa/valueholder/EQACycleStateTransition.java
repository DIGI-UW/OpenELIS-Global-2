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
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Immutable audit of cycle state transitions (FR-V2.1-21) — records the *why*
 * (manual override vs timer expiry) that the generic audit_log loses.
 * Immutability is enforced by API-surface omission: no update or delete path
 * exists anywhere; T-10/T-11 expose read-only endpoints. priorState/newState
 * are TEXT, not enums, because both state machines land here.
 */
@Getter
@Setter
@Entity
@Table(name = "eqa_cycle_state_transition")
public class EQACycleStateTransition extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_cycle_state_transition_generator")
    @SequenceGenerator(name = "eqa_cycle_state_transition_generator", sequenceName = "eqa_cycle_state_transition_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private EQACycle cycle;

    /** NULL = cycle creation. */
    @Column(name = "prior_state", columnDefinition = "TEXT")
    private String priorState;

    @Column(name = "new_state", columnDefinition = "TEXT", nullable = false)
    private String newState;

    @Enumerated(EnumType.STRING)
    @Column(name = "state_machine", nullable = false, length = 20)
    private EQAStateMachine stateMachine;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 10)
    private EQATriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_event", nullable = false, length = 40)
    private EQATriggerEvent triggerEvent;

    /** Populated when triggerType is MANUAL, NULL for AUTO. */
    @Column(name = "triggered_by")
    private Long triggeredBy;

    /** Required at API layer for off-happy-path manual transitions. */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Timestamp occurredAt;

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
