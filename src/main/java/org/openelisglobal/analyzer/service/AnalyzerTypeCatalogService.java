package org.openelisglobal.analyzer.service;

import org.springframework.security.access.prepost.PreAuthorize;

public interface AnalyzerTypeCatalogService {

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerTypeCatalogView getCatalog();

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerTypeCatalogView.TypeSummary getType(String profileId, int revision);
}
