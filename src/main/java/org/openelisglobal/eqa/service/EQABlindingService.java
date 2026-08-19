package org.openelisglobal.eqa.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAUnblindMethod;

/**
 * In-house blinding backend (OGC-612, FR-V2.4-04..08): seal a panel and
 * distribute it as standard OpenELIS orders keyed by blind code, then unblind
 * at the deadline and score every participant result against the sealed
 * targets.
 */
public interface EQABlindingService {

    /**
     * One order to create for one panel sample: which orderable test carries the
     * analysis, and (optionally) which analyst is assigned. When {@code analystId}
     * is null the service round-robins over the scheme's analyst roster
     * (FR-V2.4-03).
     */
    record BlindOrderSpec(Long panelSampleId, String testId, Long analystId) {
    }

    /**
     * FR-V2.4-04 "Seal panel &amp; distribute": seals the panel (existing
     * validation: targets present, in-house unblind date), then for every panel
     * sample creates a standard order — Sample with the blind code as its accession
     * number (FR-V2.4-15 Workplan handoff), SampleItem, NotStarted Analysis,
     * sample_eqa row linked to the cycle/round — plus a DRAFT
     * eqa_participant_result carrying the analyst assignment, and finally moves the
     * panel to DISTRIBUTED. Every panel sample must appear in {@code specs} exactly
     * once.
     *
     * @return the panel DTO plus the created accession (blind) codes
     */
    Map<String, Object> sealAndDistribute(Long panelId, List<BlindOrderSpec> specs, String sysUserId);

    /**
     * FR-V2.4-06/07: DISTRIBUTED → UNBLINDED (the state machine makes a second call
     * a no-op conflict, which is the idempotency guard), then every participant
     * result in the panel's cycle is resolved: submitted results are scored against
     * the sealed target (numeric acceptance range, else categorical exact match);
     * unsubmitted results with a value are promoted and scored; empty results are
     * marked MISSED_DEADLINE (competency event IN_HOUSE_MISSED_DEADLINE,
     * FR-V2.4-14). Unacceptable verdicts open one Follow-Up Queue row per cycle
     * (FR-V2.4-08 — never auto-NCE for in-house). The panel ends SCORED.
     */
    EQAPanel unblindAndScore(Long panelId, String sysUserId, EQAUnblindMethod method);
}
