package org.openelisglobal.compliance.service;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ComplianceReportGenerationService {

    @PreAuthorize("hasAuthority('PRIV_REPORT_EXPORT')")
    void recordGeneration(Long sampleId, String userId);

    @PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
    Optional<OffsetDateTime> getLastGenerated(Long sampleId);
}
