package org.openelisglobal.eqa.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQADismissalCategory;
import org.openelisglobal.eqa.valueholder.EQAFollowupStatus;
import org.openelisglobal.eqa.valueholder.EQAParticipantFollowup;
import org.openelisglobal.eqa.valueholder.EQAProgram;

/**
 * The Follow-Up Queue register (FR-V2.3-02, FR-V2.1-13). One row per cycle and
 * participant organization — the schema is unique on that pair — carrying a
 * JSON snapshot of the results that put the participant in the queue.
 *
 * <p>
 * Both enqueue paths land here: in-house unacceptable results routed by the
 * blinding unblind pass (FR-V2.4-08) and external questionable scores routed by
 * the tiered NCE rules (FR-V2.3-01).
 */
public interface EQAParticipantFollowupService extends BaseObjectService<EQAParticipantFollowup, Long> {

    /**
     * Open or extend this lab's queue row for a cycle. Merges into an existing row
     * rather than failing the unique constraint, because a cycle can hold several
     * panels and several scored analytes.
     *
     * @param rows one map per failing result; {@code participantResultId} anchors
     *             triage back to the result, the rest is display detail
     */
    EQAParticipantFollowup enqueueForThisLab(EQAProgram scheme, EQACycle cycle, List<Map<String, Object>> rows,
            String sysUserId);

    /**
     * Open or extend the register row for a <em>participating laboratory</em> of a
     * scheme this lab provides (T-27, FR-V2.5-05). Same table and same merge rules
     * as {@link #enqueueForThisLab}; the participant organization is what tells the
     * two registers apart.
     *
     * @param persistentFailure FR-V2.5-07: unacceptable in 2 of the participant's
     *                          last 3 cycles, which flags the row and escalates it
     *                          without waiting for a reviewer
     */
    EQAParticipantFollowup enqueueForOrganization(EQAProgram scheme, EQACycle cycle, Long participantOrgId,
            List<Map<String, Object>> rows, boolean persistentFailure, String sysUserId);

    /**
     * Open queue rows for <em>this lab's own</em> follow-ups, newest first — the
     * Follow-Up Queue page (FR-V2.3-02). Rows about other laboratories belong to
     * the provider register, not here.
     */
    List<Map<String, Object>> getQueueRows();

    /**
     * The provider-side register: every row about another laboratory, in every
     * status, newest first (FR-V2.5-05..08).
     */
    List<Map<String, Object>> getProviderRegisterRows();

    /**
     * Move a register row through triage (FR-V2.5-06): response received,
     * investigation, resolution, or removal from the programme — which also
     * withdraws the participant's enrollment. Notes are required by nothing here
     * but recorded as the resolution when given.
     *
     * @throws IllegalStateException when the target is not reachable from the row's
     *                               current status
     */
    EQAParticipantFollowup transitionStatus(Long followupId, EQAFollowupStatus target, String notes, String sysUserId);

    /**
     * Notify the participating laboratory of its follow-up (FR-V2.5-08). Answers
     * {@code emailed=false} when the organization has no contact email or the mail
     * transport refused, which is the caller's cue to hand the reviewer the CSV
     * instead.
     */
    Map<String, Object> notifyParticipant(Long followupId, String sysUserId);

    /**
     * This laboratory's own organization row, or null when nothing has ever been
     * attributed to it. It is what separates the participant queue from the
     * provider register.
     */
    Long selfOrganizationId();

    /**
     * Provider-register rows still being worked: another lab's follow-up in any
     * state except RESOLVED or REMOVED_FROM_PROGRAM.
     */
    long countOpenProviderFollowups();

    /** Marks a row escalated once its NCE exists (FR-V2.3-02). */
    EQAParticipantFollowup markEscalated(Long followupId, String sysUserId);

    /**
     * Closes a row with a required category and free-text notes, writing the
     * category-specific competency event for every result the row covers
     * (FR-V2.3-02 mapping table).
     */
    EQAParticipantFollowup dismiss(Long followupId, EQADismissalCategory category, String notes, String sysUserId);

    /** The participant-result ids a queue row covers, from its JSON snapshot. */
    List<Long> resultIdsFor(EQAParticipantFollowup followup);

    /**
     * Display label for the queue's Source column, derived from the scheme type.
     */
    String sourceLabel(EQAProgram scheme);
}
