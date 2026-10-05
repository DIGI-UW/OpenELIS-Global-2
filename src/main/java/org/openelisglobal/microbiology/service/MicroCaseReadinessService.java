package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroCaseReadinessForm;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroCaseReadinessService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroCaseReadinessForm getReadiness(String caseId);
}
