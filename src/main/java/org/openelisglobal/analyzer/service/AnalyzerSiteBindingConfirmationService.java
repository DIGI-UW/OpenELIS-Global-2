package org.openelisglobal.analyzer.service;

import org.springframework.security.access.prepost.PreAuthorize;

public interface AnalyzerSiteBindingConfirmationService {

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerSiteBindingConfirmationView confirm(AnalyzerSiteBindingSnapshot candidate, String recognitionFingerprint,
            AnalyzerSiteBindingConfirmationRequest request, String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerSiteBindingConfirmationView getStatus(AnalyzerSiteBindingSnapshot candidate, String recognitionFingerprint);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerSiteBindingVerificationAssessment assessCurrent(AnalyzerSiteBindingSnapshot candidate,
            String recognitionFingerprint);
}
