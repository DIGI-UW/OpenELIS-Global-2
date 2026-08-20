package org.openelisglobal.eqa.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStateTransition;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAPanelSourceType;
import org.openelisglobal.eqa.valueholder.EQAStateMachine;
import org.openelisglobal.eqa.valueholder.EQAStorageTemp;
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

    /**
     * The laboratories this cycle was created for, as organization ids (T-24).
     * Every reader that sizes or addresses a cycle — the prep gate's aliquot
     * arithmetic, the shipment workbench's one-row-per-participant — goes through
     * here, so there is one answer to "who is in this cycle".
     *
     * <p>
     * Cycles created before {@code eqa_cycle_participant} existed have no roster,
     * and fall back to the scheme's active enrollments: the stand-in T-25 used,
     * kept only for those rows so an in-flight cycle does not lose its participants
     * at upgrade.
     */
    List<Long> participantOrganizationIds(EQACycle cycle);

    /**
     * The five-step cycle wizard's single write (FR-V2.5-02): cycle, panel, panel
     * samples and participant roster in one transaction, then PLANNED →
     * PREP_IN_PROGRESS. Either the whole cycle exists ready for prep, or none of it
     * does — a half-created cycle would show up on the provider scheme list as a
     * cycle with no panel, which the prep gate then blocks forever.
     *
     * @throws IllegalArgumentException when a required field is missing, the scheme
     *                                  is unknown, an organization is not an active
     *                                  participant of the scheme, or a sample code
     *                                  repeats
     */
    EQACycle createProviderCycle(ProviderCycleRequest request, String sysUserId);

    /**
     * What the wizard collects, in step order: 1 cycle details, 2 panel samples +
     * source, 3 participants, 4 distribution (cold chain + expiry), 5 confirm.
     *
     * <p>
     * There is no distribution-method field: the FRS names step 4 "distribution
     * method", and what the rest of the system actually consumes from that step is
     * the panel's storage temperature — which becomes the shipping box's
     * temperature requirement — and its expiration date. Recording a free-text
     * "method" alongside those would be a column nothing reads.
     *
     * <p>
     * Nor is there a reserve: {@code eqa_panel_aliquots_chk} requires produced
     * &gt;= reserved + shipped, so a reserve cannot exist before any aliquot does.
     * Holding material back is a prep-time decision and the prep workbench already
     * owns it (FR-V2.5-12).
     */
    record ProviderCycleRequest(Long schemeId, Integer cycleNumber, String cycleName, Date plannedStartDate,
            Date plannedEndDate, String panelName, EQAPanelSourceType sourceType, String lotNumber, String vendorName,
            String vendorLot, String vendorCertificateRef, List<PanelSampleRequest> samples,
            List<Long> participantOrganizationIds, EQAStorageTemp storageTemp, Date expirationDate) {
    }

    /**
     * One panel sample, identified by the orderable test rather than the analyte
     * behind it — the same choice T-21's in-house wizard makes, and the reason
     * {@code EQAPanelService.analyteIdForTest} exists.
     *
     * <p>
     * {@code targetValue} may be null — a provider panel's target is often only
     * known at scoring time — but never blank, because the encryption converter
     * passes blanks through unencrypted.
     */
    record PanelSampleRequest(String sampleCode, String testId, String targetValue, String targetUnit,
            BigDecimal acceptanceRangeLow, BigDecimal acceptanceRangeHigh) {
    }
}
