package org.openelisglobal.eqa.service;

import static org.openelisglobal.eqa.valueholder.EQACycleStatus.CLOSED;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.DELIVERED;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.PANEL_RECEIVED;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.PLANNED;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.PREP_IN_PROGRESS;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.READY_TO_SHIP;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.READY_TO_SUBMIT;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.SCORED;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.SCORING;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.SHIPPED;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.SUBMISSIONS_CLOSED;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.SUBMISSIONS_OPEN;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.SUBMITTED;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.TESTING;

import java.sql.Timestamp;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.dao.EQACycleStateTransitionDAO;
import org.openelisglobal.eqa.dao.EQAPanelDAO;
import org.openelisglobal.eqa.dao.EQAPanelReceiptDAO;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStateTransition;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAStateMachine;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.openelisglobal.eqa.valueholder.EQATriggerEvent;
import org.openelisglobal.eqa.valueholder.EQATriggerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cycle state machines and the derived participant view (T-10).
 *
 * <p>
 * Two machines write the single eqa_cycle.status column: a participating lab
 * walks the FR-V2.1-04 path, while the lab that runs the scheme walks the
 * longer FR-V2.1-18 path. Which one applies is the caller's assertion, passed
 * in as {@link EQAStateMachine}, because the same row is read both ways.
 */
@Service
@Transactional
public class EQACycleServiceImpl extends BaseObjectServiceImpl<EQACycle, Long> implements EQACycleService {

    /** FR-V2.1-04. Terminal: CLOSED. */
    private static final Map<EQACycleStatus, Set<EQACycleStatus>> PARTICIPANT_EDGES = new EnumMap<>(
            EQACycleStatus.class);

    /** FR-V2.1-18. Terminal: CLOSED. */
    private static final Map<EQACycleStatus, Set<EQACycleStatus>> PROVIDER_EDGES = new EnumMap<>(EQACycleStatus.class);

    static {
        PARTICIPANT_EDGES.put(PLANNED, EnumSet.of(PANEL_RECEIVED));
        PARTICIPANT_EDGES.put(PANEL_RECEIVED, EnumSet.of(TESTING));
        PARTICIPANT_EDGES.put(TESTING, EnumSet.of(READY_TO_SUBMIT));
        PARTICIPANT_EDGES.put(READY_TO_SUBMIT, EnumSet.of(SUBMITTED));
        PARTICIPANT_EDGES.put(SUBMITTED, EnumSet.of(SCORED));
        PARTICIPANT_EDGES.put(SCORED, EnumSet.of(CLOSED));

        PROVIDER_EDGES.put(PLANNED, EnumSet.of(PREP_IN_PROGRESS));
        PROVIDER_EDGES.put(PREP_IN_PROGRESS, EnumSet.of(READY_TO_SHIP));
        PROVIDER_EDGES.put(READY_TO_SHIP, EnumSet.of(SHIPPED));
        PROVIDER_EDGES.put(SHIPPED, EnumSet.of(DELIVERED));
        PROVIDER_EDGES.put(DELIVERED, EnumSet.of(SUBMISSIONS_OPEN));
        PROVIDER_EDGES.put(SUBMISSIONS_OPEN, EnumSet.of(SUBMISSIONS_CLOSED));
        PROVIDER_EDGES.put(SUBMISSIONS_CLOSED, EnumSet.of(SCORING));
        PROVIDER_EDGES.put(SCORING, EnumSet.of(SCORED));
        PROVIDER_EDGES.put(SCORED, EnumSet.of(CLOSED));
    }

    @Autowired
    private EQACycleDAO eqaCycleDAO;

    @Autowired
    private EQACycleStateTransitionDAO eqaCycleStateTransitionDAO;

    @Autowired
    private EQAPanelReceiptDAO eqaPanelReceiptDAO;

    @Autowired
    private EQAPanelDAO eqaPanelDAO;

    @Autowired
    private EQAParticipantResultDAO eqaParticipantResultDAO;

    public EQACycleServiceImpl() {
        super(EQACycle.class);
    }

    @Override
    protected EQACycleDAO getBaseObjectDAO() {
        return eqaCycleDAO;
    }

    @Override
    public Set<EQACycleStatus> legalNextStates(EQACycleStatus from, EQAStateMachine machine) {
        Map<EQACycleStatus, Set<EQACycleStatus>> edges = machine == EQAStateMachine.PROVIDER ? PROVIDER_EDGES
                : PARTICIPANT_EDGES;
        return edges.getOrDefault(from, EnumSet.noneOf(EQACycleStatus.class));
    }

    @Override
    public EQACycle transition(Long cycleId, EQACycleStatus newState, EQAStateMachine machine,
            EQATriggerType triggerType, EQATriggerEvent triggerEvent, Long triggeredBy, String reason,
            String sysUserId) {
        EQACycle cycle = eqaCycleDAO.get(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("EQA Cycle not found: " + cycleId));

        EQACycleStatus priorState = cycle.getStatus();
        if (!legalNextStates(priorState, machine).contains(newState)) {
            throw new EQAInvalidTransitionException(priorState, newState,
                    "Cannot move a " + machine + " cycle from " + priorState + " to " + newState);
        }

        // FR-V2.1-21 requires a reason only for off-happy-path manual moves, but
        // AC-V2.1-19 demands one for a manual transition that IS on the happy path.
        // Requiring it for every manual transition satisfies both.
        //
        // The actor is required for the same reason: a MANUAL row is the record
        // that a person decided this, and an accreditor reading it must be able to
        // ask that person why. An unattributed manual override is worse than no
        // audit row, because it looks like one.
        if (triggerType == EQATriggerType.MANUAL) {
            if (GenericValidator.isBlankOrNull(reason)) {
                throw new IllegalArgumentException("A manual cycle transition requires a reason");
            }
            if (triggeredBy == null) {
                throw new IllegalArgumentException("A manual cycle transition requires the acting user");
            }
        }

        enforcePrepGate(cycle, priorState, newState, machine);

        cycle.setStatus(newState);
        cycle.setSysUserId(sysUserId);
        EQACycle updated = eqaCycleDAO.update(cycle);

        EQACycleStateTransition audit = new EQACycleStateTransition();
        audit.setCycle(updated);
        audit.setPriorState(priorState.name());
        audit.setNewState(newState.name());
        audit.setStateMachine(machine);
        audit.setTriggerType(triggerType);
        audit.setTriggerEvent(triggerEvent);
        audit.setTriggeredBy(triggerType == EQATriggerType.MANUAL ? triggeredBy : null);
        audit.setReason(reason);
        audit.setOccurredAt(new Timestamp(System.currentTimeMillis()));
        audit.setSysUserId(sysUserId);
        eqaCycleStateTransitionDAO.insert(audit);

        return updated;
    }

    /**
     * FR-V2.1-18 / AC-V2.1-13: a provider cycle may not become ready to ship until
     * its panel has passed homogeneity QC. This is the ISO 17043 supervisor gate —
     * without it a panel that failed QC can be marked shippable, and the audit row
     * that records it looks perfectly legitimate.
     *
     * <p>
     * A cycle with no panel at all is refused too. That check is an implementation
     * decision, not FRS text: the FRS predicate is vacuously satisfied by an empty
     * panel set, which would let a panel-less cycle ship through a gate whose whole
     * point is that something passed QC.
     *
     * <p>
     * The FR pairs this with an inventory gate,
     * {@code aliquots_produced >= participant_count + reserved_count}. That half is
     * deliberately absent: no table records which labs participate in a cycle
     * (there is no {@code eqa_cycle_participant}), so {@code participant_count} has
     * no source yet. It belongs with the V2.5 prep workbench that introduces the
     * roster. The row-level invariant {@code produced >= reserved + shipped} is a
     * different predicate and is already enforced by a DB CHECK in qa/017.
     */
    private void enforcePrepGate(EQACycle cycle, EQACycleStatus priorState, EQACycleStatus newState,
            EQAStateMachine machine) {
        if (machine != EQAStateMachine.PROVIDER || priorState != PREP_IN_PROGRESS || newState != READY_TO_SHIP) {
            return;
        }
        List<EQAPanel> panels = eqaPanelDAO.findByCycleId(cycle.getId());
        if (panels.isEmpty()) {
            throw new EQAInvalidTransitionException(priorState, newState, "Cannot ship a cycle with no panel prepared");
        }
        if (panels.stream().anyMatch(p -> !Boolean.TRUE.equals(p.getHomogeneityQcPassed()))) {
            throw new EQAInvalidTransitionException(priorState, newState,
                    "Cannot ship until every panel has passed homogeneity QC");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQACycleStateTransition> getTransitions(Long cycleId) {
        return eqaCycleStateTransitionDAO.findByCycleId(cycleId);
    }

    /**
     * FR-V2.1-18's derivation table. The table's rows overlap — a cycle can hold
     * both a draft and a submitted result — and the FRS does not say which wins, so
     * this reads most-advanced-first: once a lab has been scored, that is its state
     * regardless of what else is lying around.
     */
    @Override
    @Transactional(readOnly = true)
    public EQACycleStatus deriveParticipantState(Long cycleId, Long labEnrollmentId) {
        EQACycle cycle = eqaCycleDAO.get(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("EQA Cycle not found: " + cycleId));

        if (cycle.getStatus() == CLOSED) {
            return CLOSED;
        }

        List<EQAParticipantResult> results = eqaParticipantResultDAO.findByCycleAndEnrollment(cycleId, labEnrollmentId);

        if (results.stream().anyMatch(r -> r.getSubmissionStatus() == EQASubmissionStatus.SCORED)) {
            return SCORED;
        }
        if (results.stream().anyMatch(r -> r.getSubmissionStatus() == EQASubmissionStatus.SUBMITTED)) {
            return SUBMITTED;
        }
        if (!results.isEmpty()
                && results.stream().allMatch(r -> r.getSubmissionStatus() == EQASubmissionStatus.VALIDATED_PARTIAL)) {
            return READY_TO_SUBMIT;
        }
        if (!results.isEmpty()) {
            return TESTING;
        }
        // No results yet: the panel either arrived or it did not.
        return eqaPanelReceiptDAO.findByCycleAndEnrollment(cycleId, labEnrollmentId).isPresent() ? PANEL_RECEIVED
                : PLANNED;
    }
}
