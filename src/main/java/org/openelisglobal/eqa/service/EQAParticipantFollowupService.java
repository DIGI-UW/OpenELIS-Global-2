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
 * The Follow-Up Queue register. One row per cycle and participant organization
 * — the schema is unique on that pair — carrying a JSON snapshot of the results
 * that put the participant in the queue.
 *
 * <p>
 * Both enqueue paths land here: in-house unacceptable results routed by the
 * blinding unblind pass and external questionable scores routed by the tiered
 * NCE rules.
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
     * scheme this lab provides. Same table and same merge rules as
     * {@link #enqueueForThisLab}; the participant organization is what tells the
     * two registers apart.
     *
     * @param persistentFailure unacceptable in 2 of the participant's last 3
     *                          cycles, which flags the row and escalates it without
     *                          waiting for a reviewer
     */
    EQAParticipantFollowup enqueueForOrganization(EQAProgram scheme, EQACycle cycle, Long participantOrgId,
            List<Map<String, Object>> rows, boolean persistentFailure, String sysUserId);

    /**
     * Open queue rows for <em>this lab's own</em> follow-ups, newest first — the
     * Follow-Up Queue page. Rows about other laboratories belong to the provider
     * register, not here.
     */
    List<Map<String, Object>> getQueueRows();

    /**
     * The provider-side register: every row about another laboratory, in every
     * status, newest first.
     */
    List<Map<String, Object>> getProviderRegisterRows();

    /**
     * Move a register row through triage: response received, investigation,
     * resolution, or removal from the programme — which also withdraws the
     * participant's enrollment. Notes are required by nothing here but recorded as
     * the resolution when given.
     *
     * @throws IllegalStateException when the target is not reachable from the row's
     *                               current status
     */
    EQAParticipantFollowup transitionStatus(Long followupId, EQAFollowupStatus target, String notes, String sysUserId);

    /**
     * Notify the participating laboratory of its follow-up. Answers
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

    /**
     * Open provider-side follow-ups per participating laboratory, keyed by
     * organization id, under the same membership rule as
     * {@link #countOpenProviderFollowups()} — the register's own reading of "open",
     * rather than a second one.
     */
    Map<Long, Long> countOpenProviderFollowupsByOrganization();

    /** Marks a row escalated once its NCE exists. */
    EQAParticipantFollowup markEscalated(Long followupId, String sysUserId);

    /**
     * Closes a row with a required category and free-text notes, writing the
     * category-specific competency event for every result the row covers (the
     * dismissal-to-event mapping).
     */
    EQAParticipantFollowup dismiss(Long followupId, EQADismissalCategory category, String notes, String sysUserId);

    /** The participant-result ids a queue row covers, from its JSON snapshot. */
    List<Long> resultIdsFor(EQAParticipantFollowup followup);

    /**
     * Display label for the queue's Source column, derived from the scheme type.
     */
    String sourceLabel(EQAProgram scheme);
}
