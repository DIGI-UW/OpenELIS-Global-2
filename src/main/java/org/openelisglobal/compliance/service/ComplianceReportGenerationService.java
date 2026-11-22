package org.openelisglobal.compliance.service;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface ComplianceReportGenerationService {

    void recordGeneration(Long sampleId, String userId);

    Optional<OffsetDateTime> getLastGenerated(Long sampleId);
}
