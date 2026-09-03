package org.openelisglobal.eqa.service;

import java.util.Map;
import org.hl7.fhir.r4.model.Resource;

public interface EQAFhirSubmissionService {

    /**
     * Identifier system suffixes (appended to the instance's FHIR system) that
     * route resources between a provider and a participant through the provider's
     * store: {@code CONSIGNMENT} carries the SupplyDelivery uuid of the box the
     * panel travelled in — the one identity both instances hold — on a participant
     * report and on a score report; {@code SCORES} marks a score report so a
     * participant can search for the scores addressed to its box.
     */
    String CONSIGNMENT_SUFFIX = "/eqa/consignment";
    String SCORES_SUFFIX = "/eqa/scores";
    String SCHEME_NAME_SUFFIX = "/eqa/scheme_name";
    String CYCLE_NUMBER_SUFFIX = "/eqa/cycle_number";

    /**
     * The DiagnosticReport + Observations a participant sends for a cycle: the
     * report names the consignment, scheme and cycle number, each observation names
     * its analyte by name, so a provider on another instance can place it.
     */
    Map<String, Resource> participantSubmissionResources(Long cycleId, Long labEnrollmentId);

    /**
     * The DiagnosticReport + Observations a provider returns to one participant:
     * marked as scores for the consignment the panel travelled in, observations
     * named by analyte, each with its Z and verdict.
     */
    Map<String, Resource> scoreReturnResources(Long distributionId, Long organizationId);

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
