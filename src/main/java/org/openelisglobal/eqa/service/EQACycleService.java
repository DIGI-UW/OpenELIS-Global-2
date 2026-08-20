package org.openelisglobal.eqa.service;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStateTransition;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAStateMachine;
import org.openelisglobal.eqa.valueholder.EQATriggerEvent;
import org.openelisglobal.eqa.valueholder.EQATriggerType;

public interface EQACycleService extends BaseObjectService<EQACycle, Long> {

    /**
     * A new cycle in PLANNED (FR-V2.4-01 step 1, and the same create the provider
     * wizard needs). A null cycle number takes the scheme's next one, which is what
     * the wizard suggests anyway.
     *
     * @throws IllegalArgumentException when the scheme already has that cycle
     *                                  number (uq_eqa_cycle_scheme_number)
     */
    EQACycle create(Long schemeId, Integer cycleNumber, String cycleName, Date plannedStartDate, Date plannedEndDate,
            String sysUserId);

    /**
     * Move a cycle to a new state and record why, atomically (FR-V2.1-04 /
     * FR-V2.1-18 / FR-V2.1-21). Either the status changes and exactly one audit row
     * appears, or neither does.
     *
     * @throws EQAInvalidTransitionException when the edge is not in the machine
     * @throws IllegalArgumentException      when a manual transition arrives
     *                                       without a reason
     */
    EQACycle transition(Long cycleId, EQACycleStatus newState, EQAStateMachine machine, EQATriggerType triggerType,
            EQATriggerEvent triggerEvent, Long triggeredBy, String reason, String sysUserId);

    /** Immutable audit trail for a cycle, oldest first. */
    List<EQACycleStateTransition> getTransitions(Long cycleId);

    /**
     * This lab's view of a cycle, computed from receipts and result statuses rather
     * than stored (FR-V2.1-18). No column backs this.
     */
    EQACycleStatus deriveParticipantState(Long cycleId, Long labEnrollmentId);

    /** Same derivation for a cycle already in hand, avoiding a re-fetch. */
    EQACycleStatus deriveParticipantState(EQACycle cycle, Long labEnrollmentId);

    /**
     * Evaluate the ready-to-ship gate once: QC (AC-V2.1-13) and inventory
     * (FR-V2.5-12) together, with the per-panel arithmetic the prep workbench
     * displays. The transition enforces exactly this verdict, so the workbench
     * cannot show one gate while another is applied.
     */
    EQAPrepGate evaluatePrepGate(EQACycle cycle);
}
