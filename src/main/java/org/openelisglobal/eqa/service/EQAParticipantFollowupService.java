package org.openelisglobal.eqa.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQADismissalCategory;
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
     * scheme this lab provides (T-26 scoring, FR-V2.5-05). Same table and same
     * merge rules as {@link #enqueueForThisLab}; the participant organization is
     * what tells the two registers apart.
     *
     * @param persistentFailure FR-V2.5-07: unacceptable in 2 of the participant's
     *                          last 3 cycles, which flags the row and escalates it
     *                          without waiting for a reviewer
     */
    EQAParticipantFollowup enqueueForOrganization(EQAProgram scheme, EQACycle cycle, Long participantOrgId,
            List<Map<String, Object>> rows, boolean persistentFailure, String sysUserId);

    /** Queue rows for the Follow-Up Queue table, newest first. */
    List<Map<String, Object>> getQueueRows();

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
