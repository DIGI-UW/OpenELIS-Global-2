package org.openelisglobal.analyzer.service;

import org.springframework.security.access.prepost.PreAuthorize;

public interface AnalyzerTypeMappingService {

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerTypeMappingView getMapping(String profileId, int profileRevision);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerTypeMappingView saveMapping(String profileId, int profileRevision, AnalyzerTypeMappingUpdate update,
            String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerSiteBindingConfirmationView confirmMapping(String profileId, int profileRevision,
            AnalyzerSiteBindingConfirmationRequest request, String actor);
}
