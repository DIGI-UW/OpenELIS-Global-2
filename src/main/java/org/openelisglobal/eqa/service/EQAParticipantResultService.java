package org.openelisglobal.eqa.service;

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
     * SUBMITTED → SCORED with the provider's verdict. A QUESTIONABLE or
     * UNACCEPTABLE score on a result with an assigned analyst writes the
     * corresponding competency event (FR-V2.1-22).
     */
    EQAParticipantResult recordScore(Long resultId, EQAPerformanceStatus performance, Long eqaResultId,
            String sysUserId);

    /**
     * DRAFT/VALIDATED_PARTIAL → MISSED_DEADLINE (timer terminal). Writes the
     * scheme-type-appropriate missed-deadline competency event when an analyst is
     * assigned (FR-V2.1-22). Called by the deadline scheduler (T-16) and manually.
     */
    EQAParticipantResult markMissedDeadline(Long resultId, String sysUserId);

    /** Results for one cycle, optionally narrowed to one lab enrollment. */
    List<Map<String, Object>> getResultDtos(Long cycleId, Long labEnrollmentId);
}
