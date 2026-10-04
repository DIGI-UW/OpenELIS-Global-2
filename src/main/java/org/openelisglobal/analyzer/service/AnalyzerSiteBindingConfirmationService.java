package org.openelisglobal.analyzer.service;

import org.springframework.security.access.prepost.PreAuthorize;

public interface AnalyzerSiteBindingConfirmationService {

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerSiteBindingConfirmationView confirm(AnalyzerSiteBindingSnapshot candidate, String recognitionFingerprint,
            AnalyzerSiteBindingConfirmationRequest request, String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerSiteBindingConfirmationView getStatus(AnalyzerSiteBindingSnapshot candidate, String recognitionFingerprint);

    /**
     * Checks recorded review of the selected configuration. Current catalog
     * usability is evaluated separately for each incoming observation.
     *
     * <p>
     * Gated on PRIV_ANALYZER_IMPORT, not PRIV_ANALYZER_CONFIGURE like its siblings:
     * the only production caller is AnalyzerNormalizedResultImportServiceImpl,
     * which is itself gated on PRIV_ANALYZER_IMPORT. Requiring configure here would
     * deny the import for any role holding import alone.
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_IMPORT')")
    boolean hasMatchingConfirmation(AnalyzerSiteBindingSnapshot candidate, String recognitionFingerprint);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerSiteBindingVerificationAssessment assessCurrent(AnalyzerSiteBindingSnapshot candidate,
            String recognitionFingerprint);
}
