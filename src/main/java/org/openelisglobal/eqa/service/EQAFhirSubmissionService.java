package org.openelisglobal.eqa.service;

import java.util.Map;

public interface EQAFhirSubmissionService {

    Map<String, Object> submitResultsViaFhir(Long distributionId, Long organizationId);

    /**
     * Post one lab's participant results for a V2 cycle as a DiagnosticReport
     * bundle (FR-V2.2-05). Cycle-grain, unlike
     * {@link #submitResultsViaFhir(Long, Long)}, which serves the V1
     * distribution/eqa_result pair.
     *
     * @return false when the FHIR store refuses the bundle, so the caller can count
     *         a failed attempt and back off. A cycle with no submittable result
     *         throws instead: that is a caller error, not a transport failure, and
     *         counting it against the retry budget would hide it.
     */
    boolean submitCycleViaFhir(Long cycleId, Long labEnrollmentId);

    boolean isSubmissionLate(Long distributionId);

    Map<String, Object> approveLateSubmission(Long distributionId, Long organizationId, String justification,
            String supervisorUserId);
}
