package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroWhonetReadinessForm;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroWhonetReadinessService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroWhonetReadinessForm getReadiness(String caseId);
}
