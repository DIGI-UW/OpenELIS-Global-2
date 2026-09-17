package org.openelisglobal.microbiology.service;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Projects reviewed microbiology content into the existing standard Result
 * model. This service owns no report UI or report format; it supplies the
 * Result records consumed by the existing OpenELIS report path.
 */
public interface MicroReportProjectionService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    MicroReportProjectionResult releasePreliminary(String caseId, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    MicroReportProjectionResult releaseFinal(String caseId, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    MicroReportProjectionResult releaseAmended(String caseId, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroReportProjectionResult preview(String caseId);
}
