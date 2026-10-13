package org.openelisglobal.analyzer.service;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingConfirmation;

public interface AnalyzerMappingConfirmationService {

    AnalyzerMappingConfirmationView confirm(AnalyzerMappingSnapshot candidate, String recognitionFingerprint,
            AnalyzerMappingConfirmationRequest request, String actor);

    AnalyzerMappingConfirmationView getStatus(AnalyzerMappingSnapshot candidate, String recognitionFingerprint);

    /**
     * Checks recorded review of the selected configuration. Current catalog
     * usability is evaluated separately for each incoming observation.
     */
    boolean hasMatchingConfirmation(AnalyzerMappingSnapshot candidate, String recognitionFingerprint);

    AnalyzerMappingVerificationAssessment assessCurrent(AnalyzerMappingSnapshot candidate,
            String recognitionFingerprint);

    Optional<AnalyzerMappingConfirmation> findForMapping(String mappingId);
}
