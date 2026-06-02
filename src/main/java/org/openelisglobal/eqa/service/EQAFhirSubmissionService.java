package org.openelisglobal.eqa.service;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

public interface EQAFhirSubmissionService {

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    Map<String, Object> submitResultsViaFhir(Long distributionId, Long organizationId);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    boolean isSubmissionLate(Long distributionId);

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    Map<String, Object> approveLateSubmission(Long distributionId, Long organizationId, String justification,
            String supervisorUserId);
}
