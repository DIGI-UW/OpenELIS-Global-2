package org.openelisglobal.eqa.service;

import java.util.Map;

public interface EQAFhirSubmissionService {

    Map<String, Object> submitResultsViaFhir(Long distributionId, Long organizationId);

    boolean isSubmissionLate(Long distributionId);

    Map<String, Object> approveLateSubmission(Long distributionId, Long organizationId, String justification,
            String supervisorUserId);
}
