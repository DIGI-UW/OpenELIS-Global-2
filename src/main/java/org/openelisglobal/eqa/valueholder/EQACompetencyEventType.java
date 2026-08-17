package org.openelisglobal.eqa.valueholder;

/**
 * Analyst competency events (FR-V2.1-22) — the single source of truth for this
 * vocabulary; other requirements reference these names.
 *
 * <p>
 * A missed deadline is an absence-of-result, not a result, which is why this
 * log exists separately from eqa_participant_result. Note that not every
 * dismissal counts against an analyst in the V2.3 rollup: DISMISSED_EQUIPMENT
 * and DISMISSED_ACCEPTABLE_ON_REVIEW do not, while DISMISSED_TRANSCRIPTION and
 * DISMISSED_OTHER do.
 */
public enum EQACompetencyEventType {
    EXTERNAL_MISSED_DEADLINE, IN_HOUSE_MISSED_DEADLINE, UNACCEPTABLE_SCORE, QUESTIONABLE_SCORE, ESCALATED_TO_NCE,
    DISMISSED_EQUIPMENT, DISMISSED_TRANSCRIPTION, DISMISSED_ACCEPTABLE_ON_REVIEW, DISMISSED_OTHER
}
