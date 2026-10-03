package org.openelisglobal.analyzer.service;

public interface AnalyzerSiteBindingConfirmationService {

    AnalyzerSiteBindingConfirmationView confirm(AnalyzerSiteBindingSnapshot candidate, String recognitionFingerprint,
            AnalyzerSiteBindingConfirmationRequest request, String actor);

    AnalyzerSiteBindingConfirmationView getStatus(AnalyzerSiteBindingSnapshot candidate, String recognitionFingerprint);

    /**
     * Checks recorded review of the selected configuration. Current catalog
     * usability is evaluated separately for each incoming observation.
     */
    boolean hasMatchingConfirmation(AnalyzerSiteBindingSnapshot candidate, String recognitionFingerprint);

    AnalyzerSiteBindingVerificationAssessment assessCurrent(AnalyzerSiteBindingSnapshot candidate,
            String recognitionFingerprint);
}
