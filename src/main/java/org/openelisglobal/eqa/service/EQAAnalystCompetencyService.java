package org.openelisglobal.eqa.service;

import java.util.Map;
import org.openelisglobal.eqa.valueholder.EQAAnalystCompetencyEvent;
import org.openelisglobal.eqa.valueholder.EQACompetencyEventType;
import org.openelisglobal.eqa.valueholder.EQADismissalCategory;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;

/**
 * The single writer for analyst competency events (FR-V2.1-22). Scoring, missed
 * deadlines, NCE escalation and triage dismissal all record through here so the
 * "no analyst, no event" rule and the ISO 15189 §6.2.3 evidence trail have one
 * implementation.
 *
 * <p>
 * It is also the reader those events exist for: {@link #getCompetencyRollup()}
 * turns the log into the FR-V2.3-06 competency bands.
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

    /**
     * The Analyst Competency dashboard rollup (FR-V2.3-06): every analyst assigned
     * to PT in the trailing twelve months, their per-analyte competency band, and
     * the events behind it.
     *
     * <p>
     * Derived on read from this log unioned with the scored results it does not
     * cover, so nothing here can go stale and no second writer exists.
     *
     * @return {@code kpis} and {@code analysts}, in one read because the page
     *         renders the tiles and the table together.
     */
    Map<String, Object> getCompetencyRollup();
}
