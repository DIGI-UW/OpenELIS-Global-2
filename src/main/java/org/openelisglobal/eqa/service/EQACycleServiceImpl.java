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

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.dao.EQACycleStateTransitionDAO;
import org.openelisglobal.eqa.dao.EQAPanelDAO;
import org.openelisglobal.eqa.dao.EQAPanelReceiptDAO;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.service.EQAPrepGate.PanelRequirement;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStateTransition;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAStateMachine;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.openelisglobal.eqa.valueholder.EQATriggerEvent;
import org.openelisglobal.eqa.valueholder.EQATriggerType;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cycle state machines and the derived participant view (T-10). Two machines
 * share the single eqa_cycle.status column: participant (FR-V2.1-04) and
 * provider (FR-V2.1-18); which applies is the caller's assertion via
 * {@link EQAStateMachine}.
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

    @Autowired
    private EQAPanelSampleDAO eqaPanelSampleDAO;

    @Autowired
    private EQAProgramEnrollmentService eqaProgramEnrollmentService;

    @Autowired
    private EQAProgramService eqaProgramService;

    @Autowired
    private SystemUserService systemUserService;

    public EQACycleServiceImpl() {
        super(EQACycle.class);
    }

    @Override
    protected EQACycleDAO getBaseObjectDAO() {
        return eqaCycleDAO;
    }

    @Override
    public EQACycle create(Long schemeId, Integer cycleNumber, String cycleName, Date plannedStartDate,
            Date plannedEndDate, String sysUserId) {
        EQAProgram scheme = eqaProgramService.get(schemeId);
        List<EQACycle> existing = eqaCycleDAO.getAllMatchingOrdered("scheme.id", schemeId, "cycleNumber", true);
        int number = cycleNumber == null ? (existing.isEmpty() ? 1 : existing.get(0).getCycleNumber() + 1)
                : cycleNumber;
        // uq_eqa_cycle_scheme_number would catch this too, but as a 500 with a
        // constraint name in it rather than something a wizard can render.
        if (existing.stream().anyMatch(cycle -> cycle.getCycleNumber() == number)) {
            throw new IllegalArgumentException("Scheme " + scheme.getName() + " already has cycle " + number);
        }

        EQACycle cycle = new EQACycle();
        cycle.setScheme(scheme);
        cycle.setCycleNumber(number);
        cycle.setCycleName(GenericValidator.isBlankOrNull(cycleName) ? null : cycleName);
        cycle.setPlannedStartDate(plannedStartDate);
        cycle.setPlannedEndDate(plannedEndDate);
        cycle.setStatus(EQACycleStatus.PLANNED);
        cycle.setCreatedBy(systemUserService.get(sysUserId));
        cycle.setSysUserId(sysUserId);
        cycle.setId(eqaCycleDAO.insert(cycle));
        return cycle;
    }

    private static Set<EQACycleStatus> legalNextStates(EQACycleStatus from, EQAStateMachine machine) {
        Map<EQACycleStatus, Set<EQACycleStatus>> edges = machine == EQAStateMachine.PROVIDER ? PROVIDER_EDGES
                : PARTICIPANT_EDGES;
        return edges.getOrDefault(from, EnumSet.noneOf(EQACycleStatus.class));
    }

    @Override
    public EQACycle transition(Long cycleId, EQACycleStatus newState, EQAStateMachine machine,
            EQATriggerType triggerType, EQATriggerEvent triggerEvent, Long triggeredBy, String reason,
            String sysUserId) {
        // Row-locked read: a concurrent transition waits here, then re-reads the
        // committed status and fails the edge check instead of double-writing.
        EQACycle cycle = eqaCycleDAO.getForUpdate(cycleId)
                .orElseThrow(() -> new ObjectNotFoundException(cycleId, EQACycle.class.getName()));

        EQACycleStatus priorState = cycle.getStatus();
        if (!legalNextStates(priorState, machine).contains(newState)) {
            throw new EQAInvalidTransitionException(priorState, newState,
                    "Cannot move a " + machine + " cycle from " + priorState + " to " + newState);
        }

        // FR-V2.1-21 requires a reason for off-happy-path manual moves; AC-V2.1-19
        // requires one on the happy path too. Requiring both reason and actor on
        // every MANUAL transition satisfies both.
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
     * FR-V2.1-18 / AC-V2.1-13: a provider cycle may not reach ready_to_ship while
     * {@link #evaluatePrepGate} names anything outstanding. The refusal quotes the
     * gate's own blockers, so the operator reads the same sentences the prep
     * workbench shows.
     */
    private void enforcePrepGate(EQACycle cycle, EQACycleStatus priorState, EQACycleStatus newState,
            EQAStateMachine machine) {
        if (machine != EQAStateMachine.PROVIDER || priorState != PREP_IN_PROGRESS || newState != READY_TO_SHIP) {
            return;
        }
        EQAPrepGate gate = evaluatePrepGate(cycle);
        if (!gate.isClear()) {
            throw new EQAInvalidTransitionException(priorState, newState,
                    "Cannot ship yet: " + String.join("; ", gate.blockers()));
        }
    }

    /**
     * The gate, evaluated once for both its readers. A cycle with no panel is
     * refused — the FRS predicate is vacuously true on an empty set, which would
     * let a panel-less cycle through a gate whose point is that something passed
     * QC.
     *
     * <p>
     * The inventory half (T-25, FR-V2.5-12) sizes the cycle by the scheme's active
     * enrollments, since no table records per-cycle participants: each participant
     * needs one aliquot per panel sample, on top of what the panel holds back. The
     * row-level invariant produced >= reserved + shipped is a DB CHECK in qa/017.
     */
    @Override
    @Transactional(readOnly = true)
    public EQAPrepGate evaluatePrepGate(EQACycle cycle) {
        int participants = cycle.getScheme() == null ? 0
                : (int) eqaProgramEnrollmentService.countActiveEnrollments(cycle.getScheme().getId());
        List<EQAPanel> panels = eqaPanelDAO.getAllMatching("cycle.id", cycle.getId());

        List<PanelRequirement> requirements = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        if (panels.isEmpty()) {
            blockers.add("No panel has been prepared for this cycle");
        }
        for (EQAPanel panel : panels) {
            int samples = eqaPanelSampleDAO.getAllMatching("panel.id", panel.getId()).size();
            PanelRequirement requirement = new PanelRequirement(panel, samples,
                    samples * participants + nullToZero(panel.getAliquotsReserved()));
            requirements.add(requirement);

            if (!Boolean.TRUE.equals(panel.getHomogeneityQcPassed())) {
                blockers.add("Panel " + panel.getPanelName() + " has not passed homogeneity QC");
            }
            if (requirement.shortfall() > 0) {
                blockers.add("Panel " + panel.getPanelName() + " needs " + requirement.aliquotsNeeded()
                        + " aliquots, has " + requirement.produced());
            }
        }
        return new EQAPrepGate(participants, requirements, blockers);
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQACycleStateTransition> getTransitions(Long cycleId) {
        return eqaCycleStateTransitionDAO.getAllMatchingOrdered("cycle.id", cycleId, List.of("occurredAt", "id"),
                false);
    }

    @Override
    @Transactional(readOnly = true)
    public EQACycleStatus deriveParticipantState(Long cycleId, Long labEnrollmentId) {
        return deriveParticipantState(get(cycleId), labEnrollmentId);
    }

    /**
     * FR-V2.1-18's derivation table. Its rows overlap and the FRS does not say
     * which wins, so this reads most-advanced-first.
     */
    @Override
    @Transactional(readOnly = true)
    public EQACycleStatus deriveParticipantState(EQACycle cycle, Long labEnrollmentId) {
        if (cycle.getStatus() == CLOSED) {
            return CLOSED;
        }

        List<EQAParticipantResult> results = eqaParticipantResultDAO
                .getAllMatching(Map.of("cycle.id", cycle.getId(), "labEnrollmentId", labEnrollmentId));

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
        return eqaPanelReceiptDAO.getAllMatching(Map.of("cycle.id", cycle.getId(), "labEnrollmentId", labEnrollmentId))
                .isEmpty() ? PLANNED : PANEL_RECEIVED;
    }
}
