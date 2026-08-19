package org.openelisglobal.eqa.service;

import org.openelisglobal.eqa.valueholder.EQAAnalystCompetencyEvent;
import org.openelisglobal.eqa.valueholder.EQACompetencyEventType;
import org.openelisglobal.eqa.valueholder.EQADismissalCategory;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;

/**
 * The single writer for analyst competency events (FR-V2.1-22). Scoring, missed
 * deadlines, NCE escalation and triage dismissal all record through here so the
 * "no analyst, no event" rule and the ISO 15189 §6.2.3 evidence trail have one
 * implementation.
 */
public interface EQAAnalystCompetencyService {

    /**
     * Records an event against the result's assigned analyst.
     *
     * @param nceId    the NCE this event attributes to, or null
     * @param category the triage dismissal category, or null
     * @return the persisted event, or null when the result has no assigned analyst
     *         — {@code eqa_analyst_competency_event.analyst_id} is NOT NULL by
     *         design, because competency is an analyst log
     */
    EQAAnalystCompetencyEvent record(EQAParticipantResult result, EQACompetencyEventType type, Integer nceId,
            EQADismissalCategory category, String notes, String sysUserId);

    /** Stamps the NCE onto the event already written for a scored result. */
    void attachNce(Long participantResultId, Integer nceId);
}
