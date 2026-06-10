package org.openelisglobal.compliance.service;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ComplianceReportGenerationService {

    void recordGeneration(Long sampleId, String userId);

    Optional<LocalDateTime> getLastGenerated(Long sampleId);
}
