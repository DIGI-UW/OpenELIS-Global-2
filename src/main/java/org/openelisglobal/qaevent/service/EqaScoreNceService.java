package org.openelisglobal.qaevent.service;

import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.qaevent.valueholder.NcEvent;

/**
 * Tiered EQA score to NCE adapter (OGC-611). The counterpart of
 * {@link QcViolationNceService} for proficiency testing: it decides whether a
 * scored participant result becomes a non-conformity, a Follow-Up Queue entry,
 * or nothing at all.
 *
 * <p>
 * Scoping: only participant-role results reach this adapter.
 * {@code eqa_participant_result.lab_enrollment_id} is a FK to
 * {@code eqa_lab_program_enrollment} — this lab's own enrollments — so a remote
 * participant in a scheme this lab runs cannot produce a row here at all. That
 * makes the rule that a provider-role result never creates a local NCE
 * structural property rather than a runtime branch; provider-side
 * underperformance is tracked by the V2.5 participant follow-up register
 * instead.
 */
public interface EqaScoreNceService {

    /**
     * Both EQA trigger sources carry this prefix, which is what the register's
     * {@code ?source=eqa} deep link filters on — see
     * {@code docs/eqa/nce-deep-links.md}.
     */
    String TRIGGER_SOURCE_PREFIX = "EQA_";

    /** Trigger source for an auto-created NCE, anchored on the scored result. */
    String TRIGGER_SOURCE_EQA_UNACCEPTABLE = "EQA_UNACCEPTABLE";

    /** Trigger source for a supervisor escalation, anchored on the queue row. */
    String TRIGGER_SOURCE_EQA_FOLLOWUP = "EQA_FOLLOWUP_ESCALATION";

    /**
     * Applies the score tiers to a freshly scored result. Called from
     * participant-result scoring, inside that transaction.
     */
    void onResultScored(EQAParticipantResult result, EQAPerformanceStatus performance);

    /**
     * Escalates a Follow-Up Queue row to a non-conformity: creates the NCE, closes
     * the row, and records the competency events.
     */
    NcEvent escalateFollowup(Long followupId, String sysUserId);
}
