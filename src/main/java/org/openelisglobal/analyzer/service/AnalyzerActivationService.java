package org.openelisglobal.analyzer.service;

import org.springframework.security.access.prepost.PreAuthorize;

public interface AnalyzerActivationService {

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerActivationResult readiness(String analyzerId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerActivationResult activate(String analyzerId, String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerActivationResult reactivate(String analyzerId, String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerDeactivationResult deactivate(String analyzerId, String actor);
}
