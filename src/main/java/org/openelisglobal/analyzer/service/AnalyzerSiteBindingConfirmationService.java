package org.openelisglobal.analyzer.service;

public interface AnalyzerSiteBindingConfirmationService {

    AnalyzerSiteBindingConfirmationView confirm(AnalyzerSiteBindingSnapshot candidate, String recognitionFingerprint,
            AnalyzerSiteBindingConfirmationRequest request, String actor);

    AnalyzerSiteBindingConfirmationView getStatus(AnalyzerSiteBindingSnapshot candidate, String recognitionFingerprint);

    AnalyzerSiteBindingVerificationAssessment assessCurrent(AnalyzerSiteBindingSnapshot candidate,
            String recognitionFingerprint);
}
