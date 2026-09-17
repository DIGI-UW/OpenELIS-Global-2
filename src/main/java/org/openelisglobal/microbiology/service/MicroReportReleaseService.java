package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroReportReleaseService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    MicroCase releasePreliminary(String caseId, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    MicroCase releaseFinal(String caseId, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    MicroCase releaseAmended(String caseId, String performedBy);
}
