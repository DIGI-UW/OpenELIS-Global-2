package org.openelisglobal.analyzer.service;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingConfirmation;

public interface AnalyzerMappingConfirmationService {

    AnalyzerMappingConfirmationView confirm(AnalyzerMappingSnapshot candidate,
            AnalyzerMappingConfirmationRequest request, String actor);

    AnalyzerMappingConfirmationView getStatus(AnalyzerMappingSnapshot candidate);

    /**
     * Checks recorded review of the selected configuration. Current catalog
     * usability is evaluated separately for each incoming observation.
     */
    boolean hasMatchingConfirmation(AnalyzerMappingSnapshot candidate);

    AnalyzerMappingVerificationAssessment assessCurrent(AnalyzerMappingSnapshot candidate);

    Optional<AnalyzerMappingConfirmation> findForMapping(String mappingId);

}
