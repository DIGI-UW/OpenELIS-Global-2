package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroCaseNonconformanceRequestForm;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroCaseNonconformanceService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    MicroCaseNonconformanceResult report(String caseId, MicroCaseNonconformanceRequestForm request,
            String authenticatedUserId);
}
