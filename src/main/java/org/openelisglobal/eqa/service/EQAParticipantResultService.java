package org.openelisglobal.eqa.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;

public interface EQAParticipantResultService extends BaseObjectService<EQAParticipantResult, Long> {

    /**
     * Insert a new DRAFT, or update the editable fields of an existing DRAFT.
     * Anything past DRAFT is immutable through this path (FR-V2.1-05).
     */
    EQAParticipantResult saveDraft(EQAParticipantResult result);

    /**
     * DRAFT → VALIDATED_PARTIAL → SUBMITTED (FR-V2.1-05); SUBMITTED stamps
     * {@code submittedAt}. SCORED and MISSED_DEADLINE are refused here — they carry
     * side-effects and go through {@link #recordScore} /
     * {@link #markMissedDeadline}.
     *
     * @throws IllegalStateException when the edge is not in the lifecycle
     */
    EQAParticipantResult transitionStatus(Long resultId, EQASubmissionStatus target, String sysUserId);

    /**
     * SUBMITTED → SCORED with the provider's verdict, which is persisted on the row
     * (FR-V2.4-07) so a scored result is distinguishable from an unscored one. A
     * QUESTIONABLE or UNACCEPTABLE score on a result with an assigned analyst
     * writes the corresponding competency event (FR-V2.1-22).
     */
    EQAParticipantResult recordScore(Long resultId, EQAPerformanceStatus performance, Long eqaResultId,
            String sysUserId);

    /**
     * The same transition carrying the provider's Z-score, which the FR-V2.3-01
     * tiers read to choose between a non-conformity and the Follow-Up Queue.
     * In-house scoring has no Z by construction (FR-V2.4-07) and uses the overload
     * above.
     */
    EQAParticipantResult recordScore(Long resultId, EQAPerformanceStatus performance, BigDecimal zScore,
            Long eqaResultId, String sysUserId);

    /**
     * DRAFT/VALIDATED_PARTIAL → MISSED_DEADLINE (timer terminal). Writes the
     * scheme-type-appropriate missed-deadline competency event when an analyst is
     * assigned (FR-V2.1-22). Called by the deadline scheduler and manually.
     */
    EQAParticipantResult markMissedDeadline(Long resultId, String sysUserId);

    /** Results for one cycle, optionally narrowed to one lab enrollment. */
    List<Map<String, Object>> getResultDtos(Long cycleId, Long labEnrollmentId);

    /**
     * The lab unit that ran each result, keyed by result id: taken from the linked
     * analysis when the result came through standard result entry, falling back to
     * the scheme's own section. One query for the whole list, not one per row.
     *
     * <p>
     * A result whose section resolves nowhere is absent from the map — the caller
     * decides what an unassigned section reads as.
     */
    Map<Long, String> sectionNamesByResultId(List<EQAParticipantResult> results);
}
